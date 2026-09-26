package com.hakunakuinama.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hakunakuinama.app.domain.model.Meal
import com.hakunakuinama.app.domain.usecase.GetFavoriteMealsUseCase
import com.hakunakuinama.app.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface FavoritesUiState {
    data object Loading : FavoritesUiState
    data class Ready(val favorites: List<Meal>) : FavoritesUiState {
        val isEmpty: Boolean get() = favorites.isEmpty()
    }

    data class Error(val cause: Throwable) : FavoritesUiState
}

sealed interface FavoritesEvent {
    data class Removed(val mealName: String) : FavoritesEvent
    data object RemoveFailed : FavoritesEvent
}

/**
 * The saved-meals list.
 *
 * Shares the Channel-over-StateFlow reasoning with the recipe screen: a snackbar is an
 * event, and replaying "Removed Githeri" after a rotation would be a lie — the meal is
 * already gone.
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    getFavoriteMeals: GetFavoriteMealsUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
) : ViewModel() {

    /**
     * The declared `Flow<FavoritesUiState>` type is load-bearing, not decoration: without
     * it `map` infers `Flow<FavoritesUiState.Ready>` and the `catch` below could only
     * ever emit a `Ready`, so the error branch would not compile.
     */
    private val favoriteMeals: Flow<FavoritesUiState> =
        getFavoriteMeals().map { favorites -> FavoritesUiState.Ready(favorites) }

    val uiState: StateFlow<FavoritesUiState> = favoriteMeals
        .catch { cause -> emit(FavoritesUiState.Error(cause)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = FavoritesUiState.Loading,
        )

    private val _events = Channel<FavoritesEvent>(Channel.BUFFERED)
    val events: Flow<FavoritesEvent> = _events.receiveAsFlow()

    /**
     * Swipe-to-unfavourite.
     *
     * The guard matters: by the time the swipe lands, the row may already be gone (removed
     * from another screen, or the stream caught up late). Toggling a meal that is *not* a
     * favourite would re-add it — so we only act when it is actually in the list.
     */
    fun onRemove(mealId: Long) {
        val current = uiState.value as? FavoritesUiState.Ready ?: return
        val meal = current.favorites.firstOrNull { it.id == mealId } ?: return

        viewModelScope.launch {
            toggleFavorite(mealId)
                .onSuccess { _events.send(FavoritesEvent.Removed(meal.name)) }
                .onFailure { _events.send(FavoritesEvent.RemoveFailed) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

package com.hakunakuinama.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hakunakuinama.app.domain.model.Meal
import com.hakunakuinama.app.domain.usecase.GetMealByIdUseCase
import com.hakunakuinama.app.domain.usecase.ToggleFavoriteUseCase
import com.hakunakuinama.app.ui.navigation.ARG_RECIPE_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Recipe detail state.
 *
 * [NotFound] is a first-class state, not an error: a stale deep link or a popped back
 * stack entry is a normal outcome of navigation, and the user should see "recipe not
 * found" with a way back rather than a failure message.
 */
sealed interface RecipeDetailUiState {
    data object Loading : RecipeDetailUiState
    data object NotFound : RecipeDetailUiState

    /**
     * @param meal the recipe, including its derived cost — read `meal.costPerServingKes`
     *   in the UI. There is no stored cost field to disagree with it.
     * @param isUpdatingFavorite true while the write is in flight, so the FAB can be
     *   disabled instead of letting a double tap toggle twice and land back where it started.
     */
    data class Ready(
        val meal: Meal,
        val isUpdatingFavorite: Boolean = false,
    ) : RecipeDetailUiState {
        val isFavorite: Boolean get() = meal.isFavourite
    }

    data class Error(val cause: Throwable) : RecipeDetailUiState
}

/**
 * One-shot UI effects.
 *
 * Deliberately semantic rather than a ready-made string: the UI owns the copy (and the
 * localized resources), so a failed write can never leak a raw exception message to a
 * student looking for lunch.
 */
sealed interface RecipeDetailEvent {
    data class FavoriteChanged(val isFavourite: Boolean) : RecipeDetailEvent
    data object FavoriteFailed : RecipeDetailEvent
}

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMealById: GetMealByIdUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
) : ViewModel() {

    /**
     * Read from [SavedStateHandle], not a constructor argument: a nav argument held in a
     * plain constructor parameter is lost on process death, and the user comes back to a
     * screen that forgot which recipe it was showing. The handle is restored by the
     * framework, so the id survives.
     *
     * Read once because the route is immutable — the key never changes for a given
     * back stack entry. A missing or nonsensical id (0 or negative) is treated as
     * "not found" instead of querying for a row that cannot exist.
     */
    private val recipeId: Long? = savedStateHandle.get<Long>(ARG_RECIPE_ID)?.takeIf { it > 0L }

    val uiState: StateFlow<RecipeDetailUiState> = if (recipeId == null) {
        // No database work at all for an impossible id: skip straight to the empty state.
        MutableStateFlow(RecipeDetailUiState.NotFound)
    } else {
        getMealById(recipeId)
            .map { meal ->
                if (meal == null) {
                    RecipeDetailUiState.NotFound
                } else {
                    RecipeDetailUiState.Ready(meal = meal)
                }
            }
            .catch { cause -> emit(RecipeDetailUiState.Error(cause)) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = RecipeDetailUiState.Loading,
            )
    }

    /**
     * Snackbars are events, not state.
     *
     * A `StateFlow` would replay its last value to every new collector, so rotating the
     * device would pop "Saved to favorites" a second time. A `Channel` hands the event to
     * exactly one collector, and `BUFFERED` means an event fired while the screen is in the
     * background is still delivered when the user comes back.
     */
    private val _events = Channel<RecipeDetailEvent>(Channel.BUFFERED)
    val events: Flow<RecipeDetailEvent> = _events.receiveAsFlow()

    /**
     * Guards against a double tap. `ToggleFavoriteUseCase` is read-then-write, so two
     * rapid taps would flip the flag twice and leave the heart exactly as it started —
     * looking like the app ignored the user. Only `viewModelScope` (main dispatcher)
     * touches this flag, so no synchronisation is needed.
     */
    private var isToggleInFlight = false

    fun onFavoriteClicked() {
        val id = recipeId ?: return
        if (isToggleInFlight) return
        isToggleInFlight = true

        viewModelScope.launch {
            try {
                toggleFavorite(id)
                    .onSuccess { isFavourite -> _events.send(RecipeDetailEvent.FavoriteChanged(isFavourite)) }
                    .onFailure { _events.send(RecipeDetailEvent.FavoriteFailed) }
            } finally {
                isToggleInFlight = false
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

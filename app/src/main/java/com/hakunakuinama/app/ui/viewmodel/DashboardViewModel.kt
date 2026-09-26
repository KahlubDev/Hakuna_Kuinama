package com.hakunakuinama.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hakunakuinama.app.domain.model.Meal
import com.hakunakuinama.app.domain.usecase.GetFavoriteMealsUseCase
import com.hakunakuinama.app.domain.usecase.GetSuggestedMealUseCase
import com.hakunakuinama.app.domain.usecase.MealSuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Dashboard state.
 *
 * [Ready.suggestion] carries the slot *and* the meal on purpose. The UI must render
 * `suggestion.slot` as the header and `suggestion.meal` on the card; if it re-derived the
 * time of day itself, the two could disagree at the moment the clock crosses a slot
 * boundary, which is the exact bug Phase 2's `MealSuggestion` exists to prevent.
 *
 * A `Ready` whose `meal` is null means "no recipe for this slot" and renders as an empty
 * state, not an error. See the note on [DashboardViewModel] about the first launch.
 */
sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Ready(
        val suggestion: MealSuggestion,
        val favorites: List<Meal>,
    ) : DashboardUiState

    /** Carries the cause for logging; the UI shows its own friendly copy, never the message. */
    data class Error(val cause: Throwable) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    getSuggestedMeal: GetSuggestedMealUseCase,
    getFavoriteMeals: GetFavoriteMealsUseCase,
) : ViewModel() {

    /**
     * No timer here on purpose. `GetSuggestedMealUseCase` already re-emits when the slot
     * rolls over, so a ViewModel-level ticker would be a second clock that can disagree
     * with the first.
     *
     * `WhileSubscribed(5_000)` also keeps the upstream dead while the user is elsewhere:
     * the rollover ticker only runs while the dashboard is actually on screen.
     *
     * Known first-launch quirk: the catalogue is seeded from Room's onCreate callback, so
     * the very first emission can be an empty suggestion that becomes a meal a frame
     * later. Fixing it properly means exposing seed status from the data layer (or
     * shipping a prepackaged database) — worth it only if it is visible in practice.
     */
    /**
     * The declared `Flow<DashboardUiState>` type is load-bearing, not decoration: without
     * it `combine` infers `Flow<DashboardUiState.Ready>` and the `catch` below could only
     * ever emit a `Ready`, so the error branch would not compile.
     */
    private val readyState: Flow<DashboardUiState> = combine(
        getSuggestedMeal(),
        getFavoriteMeals(),
    ) { suggestion, favorites ->
        DashboardUiState.Ready(suggestion = suggestion, favorites = favorites)
    }

    val uiState: StateFlow<DashboardUiState> = readyState
        .catch { cause -> emit(DashboardUiState.Error(cause)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DashboardUiState.Loading,
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

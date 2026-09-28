package com.hakunakuinama.app.ui.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hakunakuinama.app.R
import com.hakunakuinama.app.domain.model.Meal
import com.hakunakuinama.app.domain.usecase.GetFavoriteMealsUseCase
import com.hakunakuinama.app.domain.usecase.GetSuggestedMealUseCase
import com.hakunakuinama.app.domain.usecase.MealSuggestion
import com.hakunakuinama.app.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Which greeting the dashboard opens with.
 *
 * Carries its own string resource instead of raw copy so the domain stays free of Android
 * and the translation lives in `strings.xml`, the same reason `Labels.kt` maps the meal
 * slots. Three buckets rather than four: "good afternoon" and "good evening" differ by
 * about an hour of daylight, and a student checking this at 17:00 is served better by
 * "karibu chakula" than by a clock reading.
 */
enum class Greeting(@StringRes val labelRes: Int) {
    MORNING(R.string.dashboard_greeting_morning),
    AFTERNOON(R.string.dashboard_greeting_afternoon),
    EVENING(R.string.dashboard_greeting_evening),
    ;

    companion object {
        /**
         * From the device's civil hour, matching the [Clock] the slot resolution uses.
         * Coarse on purpose: this is a greeting, not a schedule.
         */
        fun fromHour(hour: Int): Greeting = when (hour) {
            in 0..11 -> MORNING
            in 12..16 -> AFTERNOON
            else -> EVENING
        }
    }
}

/**
 * Dashboard state.
 *
 * [Ready.suggestion] carries the slot *and* the meal on purpose. The UI must render
 * `suggestion.slot` as the header and `suggestion.meal` on the card; if it re-derived the
 * time of day itself, the two could disagree at the moment the clock crosses a slot
 * boundary, which is the exact bug Phase 2's `MealSuggestion` exists to prevent.
 *
 * [Ready.greeting] is the same argument applied to the hello line: it is resolved here from
 * the injected [Clock] and handed over finished, so no screen reader or recomposition ever
 * runs its own clock.
 *
 * A `Ready` whose `meal` is null means "no recipe for this slot" and renders as an empty
 * state, not an error. See the note on [DashboardViewModel] about the first launch.
 */
sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Ready(
        val suggestion: MealSuggestion,
        val favorites: List<Meal>,
        val greeting: Greeting,
    ) : DashboardUiState

    /** Carries the cause for logging; the UI shows its own friendly copy, never the message. */
    data class Error(val cause: Throwable) : DashboardUiState
}

/**
 * One-shot things the dashboard did, as opposed to things it knows.
 *
 * A Channel, not a StateFlow, and the reason is the same as on the other screens: replaying
 * "could not save that" after a rotation would be a lie, because by then the write has long
 * since been retried or abandoned.
 */
sealed interface DashboardEvent {
    data object FavoriteFailed : DashboardEvent
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    getSuggestedMeal: GetSuggestedMealUseCase,
    getFavoriteMeals: GetFavoriteMealsUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val clock: Clock,
) : ViewModel() {

    /**
     * No timer here on purpose. `GetSuggestedMealUseCase` already re-emits when the slot
     * rolls over, so a ViewModel-level ticker would be a second clock that can disagree
     * with the first. The greeting rides along on those same emissions for the same reason.
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
        DashboardUiState.Ready(
            suggestion = suggestion,
            favorites = favorites,
            greeting = Greeting.fromHour(ZonedDateTime.now(clock).hour),
        )
    }

    val uiState: StateFlow<DashboardUiState> = readyState
        .catch { cause -> emit(DashboardUiState.Error(cause)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DashboardUiState.Loading,
        )

    private val _events = Channel<DashboardEvent>(Channel.BUFFERED)
    val events: Flow<DashboardEvent> = _events.receiveAsFlow()

    /**
     * Favourite the card on the dashboard.
     *
     * Reports failure rather than swallowing it. The heart is the only thing on the card
     * that changes, so a write that does not land would otherwise leave the user tapping a
     * button that appears broken, with nothing said. The message is worded to invite a
     * retry rather than to explain a database problem: this is a phone that briefly
     * dropped a write, not a bug they did anything about.
     */
    fun onToggleFavorite(mealId: Long) {
        viewModelScope.launch {
            toggleFavorite(mealId).onFailure { _events.send(DashboardEvent.FavoriteFailed) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

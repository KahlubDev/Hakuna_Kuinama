package com.hakunakuinama.app.ui.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hakunakuinama.app.R
import com.hakunakuinama.app.domain.model.Meal
import com.hakunakuinama.app.domain.usecase.GetSuggestedMealUseCase
import com.hakunakuinama.app.domain.usecase.GetWeeklyPicksUseCase
import com.hakunakuinama.app.domain.usecase.MealSuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

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
         *
         * TODO(place-in-labels): this is a second time-of-day taxonomy living next to
         * [com.hakunakuinama.app.domain.usecase.MealSlot.fromHour], and a second place
         * that maps a domain enum to a string resource — the mapping the KDoc above points
         * at `Labels.kt` for. When this is next touched, move both the enum and
         * [labelRes] into `ui/util/Labels.kt` beside `MealSlot.labelRes()`,
         * `FoodCategory.labelRes()` and `MealDifficulty.labelRes()`, so there is one
         * enum-to-resource mapping in the app rather than two. Deliberately not done here:
         * it is a relocation with no behaviour change, and this commit is about the two
         * labelling fixes.
         *
         * The two taxonomies are not the same question — a greeting is not a meal slot —
         * so they are kept separate rather than merged. Only the *placement* is wrong.
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
 * `suggestion.slot` as the eyebrow and `suggestion.meal` on the hero card; if it re-derived
 * the time of day itself, the two could disagree at the moment the clock crosses a slot
 * boundary, which is the exact bug `MealSuggestion` exists to prevent.
 *
 * [Ready.today] rides along on the same emissions for the same reason: the header prints
 * the weekday above the greeting, and a date read from the wall clock in composition could
 * greet the user "good morning" on Tuesday and label the screen Monday if it straddled
 * midnight. It is a [LocalDate] rather than a formatted string so the UI owns the
 * locale-aware day name and the translation lives with the rest of the copy.
 *
 * [Ready.picks] is the catalogue for "This week's picks", already ordered cheapest-plate
 * first by [GetWeeklyPicksUseCase].
 *
 * A `Ready` whose `meal` is null means "no recipe for this slot" and renders as an empty
 * state, not an error. See the note on [DashboardViewModel] about the first launch.
 */
sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Ready(
        val today: LocalDate,
        val suggestion: MealSuggestion,
        val picks: List<Meal>,
        val greeting: Greeting,
    ) : DashboardUiState

    /** Carries the cause for logging; the UI shows its own friendly copy, never the message. */
    data class Error(val cause: Throwable) : DashboardUiState
}

/**
 * The dashboard: today's suggested meal plus the rest of the week's catalogue.
 *
 * Stateless on purpose — it takes a [DashboardUiState] and emits callbacks, so it can be
 * previewed and screenshot-tested without Hilt, a ViewModel or a database. The route
 * composable next to it does the wiring.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    getSuggestedMeal: GetSuggestedMealUseCase,
    getWeeklyPicks: GetWeeklyPicksUseCase,
    private val clock: Clock,
) : ViewModel() {

    /**
     * No timer here on purpose. `GetSuggestedMealUseCase` already re-emits when the slot
     * rolls over, so a ViewModel-level ticker would be a second clock that can disagree
     * with the first. The greeting and the header's weekday ride along on those same
     * emissions for the same reason.
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
        getWeeklyPicks(),
    ) { suggestion, picks ->
        // One read of the clock for the whole emission. `LocalDate.now(clock.zone)` would
        // look equivalent and would be a bug: that overload takes a *zone*, not a clock, so
        // it reads the system clock and the dateline would ignore the injection the
        // greeting and the meal slot both honour — making the header untestable, and able
        // to print one weekday above a greeting for another.
        val now = ZonedDateTime.now(clock)
        DashboardUiState.Ready(
            today = now.toLocalDate(),
            suggestion = suggestion,
            picks = picks,
            greeting = Greeting.fromHour(now.hour),
        )
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

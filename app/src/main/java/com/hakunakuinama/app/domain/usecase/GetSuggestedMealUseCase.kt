package com.hakunakuinama.app.domain.usecase

import com.hakunakuinama.app.domain.model.Meal
import com.hakunakuinama.app.domain.model.MealSlot
import com.hakunakuinama.app.domain.repository.MealRepository
import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive

/**
 * The meal the Dashboard should push right now.
 *
 * [slot] travels with [meal] in one object on purpose. Returning them separately would
 * let the header say "Lunch" while the card still shows last night's dinner whenever the
 * clock ticks over a slot boundary mid-frame.
 */
data class MealSuggestion(
    val slot: MealSlot,
    val meal: Meal?,
)

/**
 * Resolves the time-of-day slot from the injected [Clock] and observes the recipe the
 * repository picked for it.
 *
 * The [Clock] is injected (never `LocalTime.now()`) so the Dashboard's suggestion can be
 * asserted on in unit tests, and so a future "plan tomorrow" feature needs no change here.
 */
class GetSuggestedMealUseCase @Inject constructor(
    private val mealRepository: MealRepository,
    private val clock: Clock,
) {

    /**
     * Breakfast 05:00–10:59, lunch 11:00–15:59, dinner 16:00–21:59, snack otherwise.
     * Single-shot; the Dashboard uses [invoke] so it also reacts to slot rollovers.
     */
    fun currentSlot(): MealSlot = MealSlot.fromHour(ZonedDateTime.now(clock).hour)

    /**
     * Emits the current suggestion, and re-emits when the slot rolls over.
     *
     * Without the ticker, a dashboard left open from 10:59 to 11:00 would keep selling
     * breakfast at lunchtime. Slots only change on the hour, so waking at the top of each
     * hour is enough: no per-minute polling, no drift, and the subscription only runs
     * while the screen is actually on screen.
     */
    @OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest is still marked experimental in coroutines 1.8.x
    operator fun invoke(): Flow<MealSuggestion> =
        slotTicker()
            .flatMapLatest { slot ->
                mealRepository.observeSuggestedMeal(slot).map { meal -> MealSuggestion(slot, meal) }
            }

    /** Emits the current slot, then again at the top of every hour. */
    private fun slotTicker(): Flow<MealSlot> = flow {
        while (currentCoroutineContext().isActive) {
            val now = ZonedDateTime.now(clock)
            emit(MealSlot.fromHour(now.hour))
            delay(msUntilNextHour(now))
        }
    }
}

/**
 * Milliseconds from [now] until the top of the next hour.
 *
 * Floored at one second: a zero-millisecond delay would turn the ticker into a spin loop
 * if the clock ever landed exactly on an hour boundary.
 */
internal fun msUntilNextHour(now: ZonedDateTime): Long {
    val elapsedMs = now.minute * 60_000L + now.second * 1_000L + now.nano / 1_000_000
    return (3_600_000L - elapsedMs).coerceAtLeast(1_000L)
}

package com.hakunakuinama.app.domain.usecase

import app.cash.turbine.test
import com.hakunakuinama.app.domain.model.MealSlot
import com.hakunakuinama.app.domain.repository.MealRepository
import com.hakunakuinama.app.testing.TestClock
import com.hakunakuinama.app.testing.testMeal
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The Dashboard's suggestion: which slot the clock says it is, and which recipe goes
 * with it.
 *
 * Two things are pinned here. The slot boundaries are a product decision — get them wrong
 * and a student is told to eat breakfast at 8pm. And the rollover is what a naive
 * implementation silently lacks: a dashboard left open across 11:00 must stop selling
 * breakfast.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetSuggestedMealUseCaseTest {

    private val repository: MealRepository = mockk()

    /** A use case whose clock is parked at a given wall-clock time. */
    private fun useCaseAt(hour: Int, minute: Int = 0, second: Int = 0): GetSuggestedMealUseCase {
        val time = TestClock.at(hour, minute, second)
        return GetSuggestedMealUseCase(repository, Clock.fixed(time.toInstant(), TestClock.NAIROBI))
    }

    // ------------------------------------------------------------ slot boundaries

    @Test
    fun `05 00 is breakfast`() {
        assertEquals(MealSlot.BREAKFAST, useCaseAt(5, 0).currentSlot())
    }

    @Test
    fun `10 59 59 is still breakfast`() {
        assertEquals(MealSlot.BREAKFAST, useCaseAt(10, 59, 59).currentSlot())
    }

    @Test
    fun `11 00 is lunch`() {
        assertEquals(MealSlot.LUNCH, useCaseAt(11, 0).currentSlot())
    }

    @Test
    fun `15 59 59 is still lunch`() {
        assertEquals(MealSlot.LUNCH, useCaseAt(15, 59, 59).currentSlot())
    }

    @Test
    fun `16 00 is dinner`() {
        assertEquals(MealSlot.DINNER, useCaseAt(16, 0).currentSlot())
    }

    @Test
    fun `21 59 59 is still dinner`() {
        assertEquals(MealSlot.DINNER, useCaseAt(21, 59, 59).currentSlot())
    }

    @Test
    fun `22 00 is a snack`() {
        assertEquals(MealSlot.SNACK, useCaseAt(22, 0).currentSlot())
    }

    @Test
    fun `02 00 in the morning is a snack, not breakfast`() {
        assertEquals(MealSlot.SNACK, useCaseAt(2, 0).currentSlot())
    }

    // ------------------------------------------------------------ bundled emission

    @Test
    fun `the suggestion bundles the slot with the recipe`() = runTest {
        every { repository.observeSuggestedMeal(any()) } returns
            flowOf(testMeal(1, "Masala Chai & Mandazi", slot = MealSlot.BREAKFAST))

        useCaseAt(6).invoke().test {
            val suggestion = awaitItem()
            // The whole point of MealSuggestion being one object: a consumer cannot render
            // a "Lunch" header above a breakfast card.
            assertEquals(MealSlot.BREAKFAST, suggestion.slot)
            assertEquals("Masala Chai & Mandazi", suggestion.meal?.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `no recipe for the slot yields a null meal rather than an error`() = runTest {
        every { repository.observeSuggestedMeal(any()) } returns flowOf(null)

        useCaseAt(6).invoke().test {
            val suggestion = awaitItem()
            assertNotNull("a null suggestion is a valid emission", suggestion)
            assertNull(suggestion.meal)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an unseeded catalogue emits null first, then the real suggestion`() = runTest {
        // The documented first-launch sequence: the catalogue is seeded from Room's
        // onCreate callback, so the first read can beat it. The UI must get a null meal it
        // can render as an empty state, then the real one - not a crash.
        every { repository.observeSuggestedMeal(any()) } returns
            flowOf(null, testMeal(1, "Masala Chai & Mandazi"))

        useCaseAt(6).invoke().test {
            assertNull("nothing is seeded yet", awaitItem().meal)

            val second = awaitItem()
            assertEquals("Masala Chai & Mandazi", second.meal?.name)
            assertEquals(
                "the slot must not drift between the two emissions",
                MealSlot.BREAKFAST,
                second.slot,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ------------------------------------------------------------------ rollover

    @Test
    fun `a dashboard open across 11 00 rolls over from breakfast to lunch`() = runTest {
        val clock = TestClock(TestClock.at(10, 59, 30), testScheduler)
        val requestedSlots = mutableListOf<MealSlot>()
        every { repository.observeSuggestedMeal(any()) } answers {
            requestedSlots += firstArg<MealSlot>()
            flowOf(testMeal(2, "Ugali & Sukuma Wiki"))
        }

        val seen = mutableListOf<MealSlot>()
        val collector = backgroundScope.launch {
            GetSuggestedMealUseCase(repository, clock).invoke().collect { seen += it.slot }
        }
        runCurrent()

        assertEquals("emits the current slot immediately", listOf(MealSlot.BREAKFAST), seen)

        // 30s to the top of the hour: the ticker sleeps until then, so nothing may happen
        // before it.
        advanceTimeBy(29_000)
        runCurrent()
        assertEquals("no re-emission mid-hour", listOf(MealSlot.BREAKFAST), seen)

        advanceTimeBy(2_000)
        runCurrent()

        assertEquals(
            "must roll over to lunch the moment the clock passes 11 00",
            listOf(MealSlot.BREAKFAST, MealSlot.LUNCH),
            seen,
        )
        assertEquals(
            "and must re-query the repository for the new slot",
            listOf(MealSlot.BREAKFAST, MealSlot.LUNCH),
            requestedSlots,
        )
        collector.cancel()
    }

    @Test
    fun `the ticker wakes hourly instead of polling`() = runTest {
        val clock = TestClock(TestClock.at(10, 0, 0), testScheduler)
        every { repository.observeSuggestedMeal(any()) } returns flowOf(testMeal(1))

        val seen = mutableListOf<MealSlot>()
        val collector = backgroundScope.launch {
            GetSuggestedMealUseCase(repository, clock).invoke().collect { seen += it.slot }
        }
        runCurrent()
        assertEquals(1, seen.size)

        // Almost an hour later: same slot, and no new emission.
        advanceTimeBy(3_599_000)
        runCurrent()
        assertEquals("one emission per hour boundary, not per minute", 1, seen.size)

        advanceTimeBy(2_000)
        runCurrent()
        assertEquals("and one when the hour does turn over", 2, seen.size)
        collector.cancel()
    }
}

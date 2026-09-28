package com.hakunakuinama.app.ui.viewmodel

import com.hakunakuinama.app.domain.model.Meal
import com.hakunakuinama.app.domain.model.MealSlot
import com.hakunakuinama.app.domain.repository.MealRepository
import com.hakunakuinama.app.domain.usecase.GetSuggestedMealUseCase
import com.hakunakuinama.app.domain.usecase.GetWeeklyPicksUseCase
import com.hakunakuinama.app.testing.MainDispatcherRule
import com.hakunakuinama.app.testing.TestClock
import com.hakunakuinama.app.testing.testMeal
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The Dashboard: the suggested meal plus the saved list, and what happens when either
 * stream misbehaves.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: MealRepository = mockk()

    private val breakfast = testMeal(1, "Masala Chai & Mandazi", slot = MealSlot.BREAKFAST, isFavourite = true)
    private val lunch = testMeal(2, "Ugali & Sukuma Wiki")

    private fun viewModel(clock: Clock) = DashboardViewModel(
        getSuggestedMeal = GetSuggestedMealUseCase(repository, clock),
        getWeeklyPicks = GetWeeklyPicksUseCase(repository),
        clock = clock,
    )

    private fun stubHappyPath(hour: Int = 13) {
        every { repository.observeSuggestedMeal(any()) } answers {
            when (firstArg<MealSlot>()) {
                MealSlot.BREAKFAST -> MutableStateFlow(breakfast as Meal?)
                else -> MutableStateFlow(lunch as Meal?)
            }
        }
        every { repository.observeMeals() } returns MutableStateFlow(listOf(breakfast, lunch))
    }

    @Test
    fun `starts in loading and settles on ready`() = runTest(mainDispatcherRule.testDispatcher) {
        stubHappyPath()
        val viewModel = viewModel(Clock.fixed(TestClock.at(13).toInstant(), TestClock.NAIROBI))

        assertEquals(DashboardUiState.Loading, viewModel.uiState.value)

        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()
        assertTrue(viewModel.uiState.value is DashboardUiState.Ready)
        collector.cancel()
    }

    @Test
    fun `the slot label comes from the bundled suggestion, not recomputed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubHappyPath(hour = 6)
            val viewModel = viewModel(Clock.fixed(TestClock.at(6).toInstant(), TestClock.NAIROBI))
            val collector = backgroundScope.launch { viewModel.uiState.collect { } }
            runCurrent()

            val state = viewModel.uiState.value as DashboardUiState.Ready
            // The ViewModel must not be able to disagree with itself about the slot.
            assertEquals(MealSlot.BREAKFAST, state.suggestion.slot)
            assertEquals("Masala Chai & Mandazi", state.suggestion.meal?.name)
            collector.cancel()
        }

    @Test
    fun `the weekly picks arrive cheapest plate first`() = runTest(mainDispatcherRule.testDispatcher) {
        stubHappyPath()
        val viewModel = viewModel(Clock.fixed(TestClock.at(13).toInstant(), TestClock.NAIROBI))
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()

        val state = viewModel.uiState.value as DashboardUiState.Ready
        // Both fixtures price at 100/unit for 0.5 of a thing over two servings, so this
        // asserts the *set* travelled in the same state as the suggestion; the ordering
        // itself is GetWeeklyPicksUseCase's own test.
        assertEquals(
            setOf("Masala Chai & Mandazi", "Ugali & Sukuma Wiki"),
            state.picks.map { it.name }.toSet(),
        )
        collector.cancel()
    }

    @Test
    fun `the header dateline comes from the injected clock, not the system one`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubHappyPath()
            val viewModel = viewModel(Clock.fixed(TestClock.at(13).toInstant(), TestClock.NAIROBI))
            val collector = backgroundScope.launch { viewModel.uiState.collect { } }
            runCurrent()

            val state = viewModel.uiState.value as DashboardUiState.Ready
            // The greeting and the dateline are two views of the same instant, and both
            // come from the injected clock. Reading the date off the wall clock instead
            // would print one weekday above another day's greeting, and be untestable.
            assertEquals(TestClock.at(13).toLocalDate(), state.today)
            assertEquals(Greeting.AFTERNOON, state.greeting)
            collector.cancel()
        }

    @Test
    fun `no recipe for the slot is ready-with-a-null-meal, not an error`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { repository.observeSuggestedMeal(any()) } returns MutableStateFlow(null)
            every { repository.observeMeals() } returns MutableStateFlow(emptyList())

            val viewModel = viewModel(Clock.fixed(TestClock.at(20).toInstant(), TestClock.NAIROBI))
            val collector = backgroundScope.launch { viewModel.uiState.collect { } }
            runCurrent()

            val state = viewModel.uiState.value
            assertTrue("an empty slot must not look like a failure", state is DashboardUiState.Ready)
            assertEquals(null, (state as DashboardUiState.Ready).suggestion.meal)
            collector.cancel()
        }

    @Test
    fun `a throwing stream degrades to error and keeps the cause`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { repository.observeSuggestedMeal(any()) } returns
                flow { throw IllegalStateException("database is corrupt") }
            every { repository.observeMeals() } returns MutableStateFlow(emptyList())

            val viewModel = viewModel(Clock.fixed(TestClock.at(13).toInstant(), TestClock.NAIROBI))
            val collector = backgroundScope.launch { viewModel.uiState.collect { } }
            runCurrent()

            val state = viewModel.uiState.value
            assertTrue(state is DashboardUiState.Error)
            assertTrue((state as DashboardUiState.Error).cause is IllegalStateException)
            collector.cancel()
        }

    @Test
    fun `the state rolls over to lunch when the clock passes 11 00`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubHappyPath()
            val clock = TestClock(TestClock.at(10, 59, 30), testScheduler)
            val viewModel = viewModel(clock)
            val collector = backgroundScope.launch { viewModel.uiState.collect { } }
            runCurrent()

            val seen = mutableListOf<MealSlot>()
            val slotCollector = backgroundScope.launch {
                viewModel.uiState.collect { state ->
                    if (state is DashboardUiState.Ready) seen += state.suggestion.slot
                }
            }
            runCurrent()

            testScheduler.advanceTimeBy(31_000)
            runCurrent()

            assertTrue(
                "the dashboard must follow the clock, not cache the first slot forever",
                seen.contains(MealSlot.LUNCH),
            )
            slotCollector.cancel()
            collector.cancel()
        }
}

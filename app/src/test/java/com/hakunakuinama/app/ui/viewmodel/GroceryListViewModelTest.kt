package com.hakunakuinama.app.ui.viewmodel

import com.hakunakuinama.app.domain.model.BudgetSummary
import com.hakunakuinama.app.domain.model.GroceryItem
import com.hakunakuinama.app.domain.model.GroceryPlan
import com.hakunakuinama.app.domain.model.Weeks
import com.hakunakuinama.app.domain.repository.MealRepository
import com.hakunakuinama.app.domain.usecase.GenerateGroceryListUseCase
import com.hakunakuinama.app.domain.usecase.GetBudgetSummaryUseCase
import com.hakunakuinama.app.domain.usecase.GetWeeklyGroceryListUseCase
import com.hakunakuinama.app.domain.usecase.SetGroceryItemCheckedUseCase
import com.hakunakuinama.app.testing.MainDispatcherRule
import com.hakunakuinama.app.testing.TestClock
import com.hakunakuinama.app.domain.model.FoodCategory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The shopping-list sheet: generating a list, revealing the sheet, and ticking lines off.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GroceryListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: MealRepository = mockk()

    private val items = MutableStateFlow<List<GroceryItem>>(emptyList())
    private val summary = MutableStateFlow(BudgetSummary())

    /** The plan the ViewModel actually asked for, captured from the repository call. */
    private val capturedPlan = slot<GroceryPlan>()

    private fun item(id: Long, name: String, quantity: Double = 1.0, checked: Boolean = false) = GroceryItem(
        id = id,
        name = name,
        emoji = "🌽",
        category = FoodCategory.STAPLE,
        quantity = quantity,
        unit = "kg",
        unitPriceKes = 120.0,
        isChecked = checked,
        sourceMealIds = listOf(1L),
        weekStartEpochDay = LocalDate.of(2026, 9, 28).toEpochDay(),
    )

    @org.junit.Before
    fun setUp() {
        every { repository.observeGroceryList(any()) } returns items
        every { repository.observeBudgetSummary(any()) } returns summary
        coEvery { repository.generateGroceryList(capture(capturedPlan)) } returns Result.success(3)
        coEvery { repository.setGroceryItemChecked(any(), any()) } returns Unit
    }

    private fun viewModel(hour: Int = 13) = GroceryListViewModel(
        getWeeklyGroceryList = GetWeeklyGroceryListUseCase(repository),
        getBudgetSummary = GetBudgetSummaryUseCase(repository),
        generateGroceryList = GenerateGroceryListUseCase(repository),
        setGroceryItemChecked = SetGroceryItemCheckedUseCase(repository),
        clock = java.time.Clock.fixed(TestClock.at(hour).toInstant(), ZoneId.of("Africa/Nairobi")),
    )

    @Test
    fun `loads empty and reports the week as empty, not broken`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = viewModel()
            val collector = backgroundScope.launch { viewModel.uiState.collect { } }
            runCurrent()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.isEmpty)
            assertFalse("the sheet must not open on its own", state.isSheetVisible)
            collector.cancel()
        }

    @Test
    fun `generating for a recipe opens the sheet and announces the line count`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = viewModel()
            val events = mutableListOf<GroceryListEvent>()
            backgroundScope.launch { viewModel.uiState.collect { } }
            backgroundScope.launch { viewModel.events.collect { events += it } }
            runCurrent()

            viewModel.onGenerateForRecipe(mealId = 7L)
            runCurrent()

            assertEquals(listOf(GroceryListEvent.ListGenerated(3)), events)
            assertTrue("generating must reveal the list", viewModel.uiState.value.isSheetVisible)
        }

    @Test
    fun `the plan is snapped to the Monday of the current week`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = viewModel()
            backgroundScope.launch { viewModel.uiState.collect { } }
            runCurrent()

            viewModel.onGenerateForRecipe(mealId = 7L)
            runCurrent()

            val plan = capturedPlan.captured
            assertEquals(listOf(7L), plan.mealIds)
            // TestClock.at() is pinned to 2026-09-26, a Saturday, whose week began Mon 21st.
            assertEquals(LocalDate.of(2026, 9, 21), plan.weekStart)
            assertEquals(plan.weekStart, Weeks.startOf(plan.weekStart))
        }

    @Test
    fun `the week is resolved from the injected clock, not the system clock`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Late Saturday night: still the same week, not one about to roll over.
            val sundayNight = viewModel(hour = 23)
            backgroundScope.launch { sundayNight.uiState.collect { } }
            runCurrent()

            sundayNight.onGenerateForRecipe(mealId = 7L)
            runCurrent()

            assertEquals(
                "a Saturday at 23:00 must still write to the week that is ending",
                LocalDate.of(2026, 9, 21),
                capturedPlan.captured.weekStart,
            )
        }

    @Test
    fun `a failed generation reports the failure and leaves the sheet closed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { repository.generateGroceryList(any()) } returns
                Result.failure(IllegalStateException("no meals"))

            val viewModel = viewModel()
            val events = mutableListOf<GroceryListEvent>()
            backgroundScope.launch { viewModel.uiState.collect { } }
            backgroundScope.launch { viewModel.events.collect { events += it } }
            runCurrent()

            viewModel.onGenerateForRecipe(mealId = 7L)
            runCurrent()

            assertEquals(listOf(GroceryListEvent.GenerateFailed), events)
            assertFalse("a failure must not flash an empty sheet at the user", viewModel.uiState.value.isSheetVisible)
        }

    @Test
    fun `ticking a line writes through to the database`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()

        viewModel.onItemChecked(itemId = 4L, checked = true)
        runCurrent()

        coVerify { repository.setGroceryItemChecked(4L, true) }
    }

    @Test
    fun `the sheet can be opened and dismissed independently of generating`() =
        runTest(mainDispatcherRule.testDispatcher) {
            items.value = listOf(item(1, "Maize flour"))
            summary.value = BudgetSummary(plannedKes = 120.0, lineCount = 1)

            val viewModel = viewModel()
            val collector = backgroundScope.launch { viewModel.uiState.collect { } }
            runCurrent()

            viewModel.onSheetRequested()
            runCurrent()
            assertTrue(viewModel.uiState.value.isSheetVisible)
            assertEquals(1, viewModel.uiState.value.items.size)

            viewModel.onSheetDismissed()
            runCurrent()
            assertFalse(viewModel.uiState.value.isSheetVisible)
            // The list itself must survive a dismiss - the user is coming back to it.
            assertEquals(1, viewModel.uiState.value.items.size)
            collector.cancel()
        }
}

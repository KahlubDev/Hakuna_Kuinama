package com.hakunakuinama.app.ui.viewmodel

import com.hakunakuinama.app.domain.model.MealMatch
import com.hakunakuinama.app.domain.repository.MealRepository
import com.hakunakuinama.app.domain.usecase.GetIngredientsBySelectionUseCase
import com.hakunakuinama.app.domain.usecase.GetIngredientsUseCase
import com.hakunakuinama.app.testing.MainDispatcherRule
import com.hakunakuinama.app.testing.testMeal
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The Menu Builder's selection, and the ranking it drives.
 *
 * The repository is a MockK mock rather than a hand-written fake on purpose: the
 * interesting assertion is *how many times the scoring actually runs*, so the test needs
 * to sit at the boundary and count. A fake that quietly recomputes internally would hide
 * exactly the bug this is looking for.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MenuBuilderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: MealRepository = mockk()

    private val catalogue = listOf(
        com.hakunakuinama.app.testing.testIngredient(1, "Maize flour"),
        com.hakunakuinama.app.testing.testIngredient(2, "Sukuma wiki"),
        com.hakunakuinama.app.testing.testIngredient(3, "Tomatoes"),
        com.hakunakuinama.app.testing.testIngredient(4, "Onions"),
    )

    private val meals = listOf(
        testMeal(10, "Ugali & Sukuma Wiki", ingredientIds = listOf(1, 2, 3, 4)),
        testMeal(11, "Chapati & Beans", ingredientIds = listOf(1, 2)),
    )

    /** Mirrors MealRepositoryImpl.MIN_MATCH_PERCENT, which is private to the repository. */
    private val MIN_MATCH_PERCENT = 25

    /** Every distinct selection the scoring layer was asked to score. */
    private val scoredSelections = mutableListOf<Set<Long>>()

    @Before
    fun setUp() {
        every { repository.observeIngredients() } returns kotlinx.coroutines.flow.flowOf(catalogue)
        every { repository.observeMatches(any()) } answers {
            val upstream = firstArg<kotlinx.coroutines.flow.Flow<Set<Long>>>()
            flow {
                upstream.collect { selection ->
                    scoredSelections += selection
                    // Mirrors the repository's MIN_MATCH_PERCENT floor. Without it this
                    // stub always returns every recipe, so "nothing matches" is
                    // unrepresentable and the no-results state can never be tested.
                    emit(
                        meals.map { MealMatch.of(it, selection) }
                            .filter { it.matchPercentage >= MIN_MATCH_PERCENT },
                    )
                }
            }
        }
    }

    private fun viewModel() = MenuBuilderViewModel(
        getIngredients = GetIngredientsUseCase(repository),
        getIngredientsBySelection = GetIngredientsBySelectionUseCase(repository),
    )

    // -------------------------------------------------------- empty selection

    @Test
    fun `an untouched builder shows the prompt, not every recipe`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse("loading must resolve before anything is shown", state.isLoading)
        assertTrue(
            "with nothing ticked the screen must prompt, not list all recipes",
            state.showSelectionPrompt,
        )
        assertEquals("the full chip catalogue is available immediately", 4, state.ingredients.size)
        collector.cancel()
    }

    @Test
    fun `an empty selection scores nothing as a match`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()

        assertTrue(
            "no recipe can be 'cook now' on an empty kitchen",
            viewModel.uiState.value.matches.none { it.canCookNow },
        )
        collector.cancel()
    }

    // -------------------------------------------------------------- selection

    @Test
    fun `ticking an ingredient ranks the recipes against it`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()

        viewModel.onIngredientToggled(1L)
        viewModel.onIngredientToggled(2L)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(setOf(1L, 2L), state.selectedIngredientIds)
        assertFalse("the prompt must disappear once something is ticked", state.showSelectionPrompt)
        // Meal 10 needs four ingredients and only two are ticked; meal 11 needs exactly
        // those two.
        assertEquals(50, state.matches.first { it.meal.id == 10L }.matchPercentage)
        assertEquals(100, state.matches.first { it.meal.id == 11L }.matchPercentage)
        collector.cancel()
    }

    @Test
    fun `a partial selection scores a partial match`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()

        viewModel.onIngredientToggled(1L)
        runCurrent()

        // 1 of the 4 ingredients the Ugali recipe needs.
        assertEquals(25, viewModel.uiState.value.matches.first { it.meal.id == 10L }.matchPercentage)
        collector.cancel()
    }

    @Test
    fun `tapping the same chip twice deselects it and cannot duplicate it`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()

        viewModel.onIngredientToggled(1L)
        runCurrent()
        viewModel.onIngredientToggled(1L)
        runCurrent()

        val selection = viewModel.uiState.value.selectedIngredientIds
        assertTrue("a second tap must deselect", selection.isEmpty())
        assertTrue("the prompt must come back", viewModel.uiState.value.showSelectionPrompt)
        collector.cancel()
    }

    @Test
    fun `clear empties the selection in a single step`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()

        viewModel.onIngredientToggled(1L)
        viewModel.onIngredientToggled(2L)
        viewModel.onIngredientToggled(3L)
        runCurrent()
        val before = scoredSelections.size

        viewModel.onSelectionCleared()
        runCurrent()

        assertEquals(emptySet<Long>(), viewModel.uiState.value.selectedIngredientIds)
        assertEquals(
            "clearing is one state change, so exactly one more scoring run",
            before + 1,
            scoredSelections.size,
        )
        collector.cancel()
    }

    // ------------------------------------------------------- one score per change

    @Test
    fun `each distinct selection triggers exactly one scoring run`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()

        val before = scoredSelections.size

        viewModel.onIngredientToggled(1L)
        runCurrent()
        viewModel.onIngredientToggled(2L)
        runCurrent()
        viewModel.onIngredientToggled(2L) // back to {1}
        runCurrent()

        val added = scoredSelections.drop(before)
        assertEquals(
            "four state changes, four scoring runs, and only three distinct selections",
            listOf(setOf(1L), setOf(1L, 2L), setOf(1L)),
            added,
        )
        collector.cancel()
    }

    @Test
    fun `the selection is a Set, so the same ingredients in another order is the same state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = viewModel()
            val collector = backgroundScope.launch { viewModel.uiState.collect { } }
            runCurrent()
            val before = scoredSelections.size

            // Ticked in the opposite order to the test above, so the same two ids end up in
            // the Set in a different insertion order. Because the ViewModel holds a Set, it
            // cannot express "same set, different order" as a single change — so the dedupe
            // itself is tested in GetIngredientsBySelectionUseCaseTest, where a List can
            // actually carry the ordering.
            viewModel.onIngredientToggled(2L)
            runCurrent()
            viewModel.onIngredientToggled(1L)
            runCurrent()

            assertEquals(
                "the final selection is the same two ingredients",
                setOf(1L, 2L),
                viewModel.uiState.value.selectedIngredientIds,
            )
            assertEquals(
                "and it is scored again, because the selection genuinely changed twice",
                before + 2,
                scoredSelections.size,
            )
            collector.cancel()
        }

    @Test
    fun `a selection that matches nothing surfaces the no-results state`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()

        // Only ingredients no seeded recipe uses.
        viewModel.onIngredientToggled(999L)
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue("ticked but no match must be distinguishable from not ticking", state.showNoResults)
        assertFalse(state.showSelectionPrompt)
        collector.cancel()
    }
}

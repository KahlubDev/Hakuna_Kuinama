package com.hakunakuinama.app.domain.usecase

import com.hakunakuinama.app.domain.repository.MealRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Ranking the ticked ingredients against the catalogue.
 *
 * The "same set, different order" case lives here rather than in the ViewModel test,
 * because this is the layer where order can exist at all. `MenuBuilderViewModel` holds a
 * `Set<Long>`, so it is structurally incapable of producing a reordered selection — a test
 * over it would pass no matter what the dedupe did.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetIngredientsBySelectionUseCaseTest {

    private val repository: MealRepository = mockk()

    private val meals = listOf(
        testMeal(10, "Ugali & Sukuma Wiki", ingredientIds = listOf(1, 2, 3, 4)),
        testMeal(11, "Chapati & Beans", ingredientIds = listOf(1, 2)),
    )

    private val scoredSelections = mutableListOf<Set<Long>>()

    @Before
    fun setUp() {
        every { repository.observeMatches(any()) } answers {
            val upstream = firstArg<kotlinx.coroutines.flow.Flow<Set<Long>>>()
            flow {
                upstream.collect { selection ->
                    scoredSelections += selection
                    emit(meals.map { com.hakunakuinama.app.domain.model.MealMatch.of(it, selection) })
                }
            }
        }
    }

    private fun useCase() = GetIngredientsBySelectionUseCase(repository)

    @Test
    fun `the same set in a different order is scored once`() = runTest {
        val selection = MutableStateFlow(listOf(1L, 2L, 3L))
        val collector = backgroundScope.launch { useCase().observe(selection).collect { } }
        runCurrent()
        assertEquals(1, scoredSelections.size)

        // Same ingredients, opposite order. Without distinctUntilChanged the whole
        // catalogue is re-scored for a set that is equal to the one already ranked.
        selection.value = listOf(3L, 2L, 1L)
        runCurrent()

        assertEquals(
            "an equal set must not trigger a second scoring run",
            1,
            scoredSelections.size,
        )
        assertEquals(setOf(1L, 2L, 3L), scoredSelections.single())
        collector.cancel()
    }

    @Test
    fun `a genuinely different selection is scored again`() = runTest {
        val selection = MutableStateFlow(listOf(1L, 2L))
        val collector = backgroundScope.launch { useCase().observe(selection).collect { } }
        runCurrent()

        selection.value = listOf(1L, 2L, 3L, 4L)
        runCurrent()

        assertEquals(2, scoredSelections.size)
        assertEquals(setOf(1L, 2L, 3L, 4L), scoredSelections.last())
        collector.cancel()
    }

    @Test
    fun `a full selection is cookable and an empty one is not`() = runTest {
        val useCase = useCase()

        val full = useCase(listOf(1L, 2L, 3L, 4L))
        assertTrue(full.first { it.meal.id == 10L }.canCookNow)
        assertEquals(100, full.first { it.meal.id == 10L }.matchPercentage)

        val empty = useCase(emptyList())
        assertTrue(empty.none { it.canCookNow })
        assertEquals(0, empty.first { it.meal.id == 10L }.matchPercentage)
    }

    @Test
    fun `unknown ingredient ids are ignored rather than crashing`() = runTest {
        val matches = useCase().invoke(listOf(999L))
        assertTrue(matches.none { it.canCookNow })
    }
}

package com.hakunakuinama.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.hakunakuinama.app.domain.repository.MealRepository
import com.hakunakuinama.app.domain.usecase.GetMealByIdUseCase
import com.hakunakuinama.app.domain.usecase.ToggleFavoriteUseCase
import com.hakunakuinama.app.testing.MainDispatcherRule
import com.hakunakuinama.app.testing.testMeal
import com.hakunakuinama.app.ui.navigation.ARG_RECIPE_ID
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
 * Recipe detail: resolving the id, the not-found state, and the favourite write.
 *
 * The favourite write is the interesting part. A failed database write is something to
 * tell the user about, not a reason to take the screen down — and the test asserts both
 * halves of that: the failure surfaces as an event, and the state is left intact.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecipeDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: MealRepository = mockk()

    private val githeri = testMeal(
        id = 7L,
        name = "Githeri",
        ingredientIds = listOf(1, 2),
        isFavourite = false,
    )

    @Before
    fun setUp() {
        every { repository.observeMeal(7L) } returns flowOf(githeri)
        every { repository.observeIsFavourite(7L) } returns flowOf(false)
        coEvery { repository.setFavourite(any(), any()) } returns Unit
    }

    private fun viewModel(arguments: Map<String, Any?> = mapOf(ARG_RECIPE_ID to 7L)) =
        RecipeDetailViewModel(
            savedStateHandle = SavedStateHandle(arguments),
            getMealById = GetMealByIdUseCase(repository),
            toggleFavorite = ToggleFavoriteUseCase(repository),
        )

    // ------------------------------------------------- SavedStateHandle resolution

    @Test
    fun `the recipe id is read from SavedStateHandle`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue("a valid id must resolve to a recipe", state is RecipeDetailUiState.Ready)
        assertEquals("Githeri", (state as RecipeDetailUiState.Ready).meal.name)
        collector.cancel()
    }

    @Test
    fun `loading is emitted first, then the recipe`() = runTest(mainDispatcherRule.testDispatcher) {
        // stateIn always replays its initialValue, so the first thing the UI sees is
        // Loading even when the database answers instantly. Pinned here because "show a
        // spinner, then swap it" is a real transition, not an implementation detail.
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(RecipeDetailUiState.Loading, awaitItem())
            assertTrue(awaitItem() is RecipeDetailUiState.Ready)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a missing navigation argument is not found, not an error`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel(arguments = emptyMap())
        assertEquals(
            RecipeDetailUiState.NotFound,
            viewModel.uiState.value,
        )
        coVerify(exactly = 0) { repository.observeMeal(any()) }
    }

    @Test
    fun `a zero or negative id short-circuits without touching the database`() = runTest(mainDispatcherRule.testDispatcher) {
        assertEquals(RecipeDetailUiState.NotFound, viewModel(mapOf(ARG_RECIPE_ID to 0L)).uiState.value)
        assertEquals(RecipeDetailUiState.NotFound, viewModel(mapOf(ARG_RECIPE_ID to -7L)).uiState.value)
        coVerify(exactly = 0) { repository.observeMeal(any()) }
    }

    @Test
    fun `a null meal emission is the not-found state, never an error`() = runTest(mainDispatcherRule.testDispatcher) {
        every { repository.observeMeal(404L) } returns flowOf(null)

        val viewModel = viewModel(mapOf(ARG_RECIPE_ID to 404L))
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()
        assertEquals(RecipeDetailUiState.NotFound, viewModel.uiState.value)
        collector.cancel()
    }

    @Test
    fun `a throwing stream becomes an error state instead of crashing`() = runTest(mainDispatcherRule.testDispatcher) {
        every { repository.observeMeal(7L) } returns
            flow { throw IllegalStateException("database is corrupt") }

        val viewModel = viewModel()
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()
        val state = viewModel.uiState.value
        assertTrue("must degrade to Error, not crash the screen", state is RecipeDetailUiState.Error)
        assertTrue(
            "and carry the cause for logging",
            (state as RecipeDetailUiState.Error).cause is IllegalStateException,
        )
        collector.cancel()
    }

    @Test
    fun `the cost shown to the user is the derived per-plate value`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()
        val state = viewModel.uiState.value as RecipeDetailUiState.Ready
        // 2 ingredients x 0.5kg x 100 KES = 100 KES per batch, 2 servings = 50 per plate.
        assertEquals(50.0, state.meal.costPerServingKes, 0.001)
        collector.cancel()
    }

    // -------------------------------------------------------------- favourite write

    @Test
    fun `toggling the heart saves the recipe and says so`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        // Only the uiState collector runs here. A second collector on the event channel
        // would race the turbine block below for the single event and make it flaky.
        backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()

        viewModel.onFavoriteClicked()
        runCurrent()

        coVerify { repository.setFavourite(7L, true) }
        viewModel.events.test {
            assertEquals(RecipeDetailEvent.FavoriteChanged(true), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a failed favourite write surfaces as an event and leaves the recipe loaded`() = runTest(mainDispatcherRule.testDispatcher) {
        every { repository.observeMeal(7L) } returns
            flowOf(githeri.copy(isFavourite = true))
        coEvery { repository.setFavourite(any(), any()) } throws IllegalStateException("disk is full")

        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()

        viewModel.onFavoriteClicked()
        runCurrent()

        // No exception escaped: the ViewModel is still alive and still showing the recipe.
        val state = viewModel.uiState.value
        assertTrue("the screen must survive a failed write", state is RecipeDetailUiState.Ready)
        assertTrue((state as RecipeDetailUiState.Ready).isFavorite)

        viewModel.events.test {
            assertEquals(
                RecipeDetailEvent.FavoriteFailed,
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a double tap cannot toggle twice and land back where it started`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()

        // Two taps before the coroutine gets a chance to run.
        viewModel.onFavoriteClicked()
        viewModel.onFavoriteClicked()
        runCurrent()

        coVerify(exactly = 1) { repository.setFavourite(any(), any()) }
    }

    @Test
    fun `a second tap after the write completes does toggle back`() = runTest(mainDispatcherRule.testDispatcher) {
        every { repository.observeIsFavourite(7L) } returns flowOf(true)

        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()

        viewModel.onFavoriteClicked()
        runCurrent()

        coVerify { repository.setFavourite(7L, false) }
    }
}

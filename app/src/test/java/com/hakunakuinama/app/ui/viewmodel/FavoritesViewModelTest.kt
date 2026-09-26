package com.hakunakuinama.app.ui.viewmodel

import com.hakunakuinama.app.domain.model.Meal
import com.hakunakuinama.app.domain.repository.MealRepository
import com.hakunakuinama.app.domain.usecase.GetFavoriteMealsUseCase
import com.hakunakuinama.app.domain.usecase.ToggleFavoriteUseCase
import com.hakunakuinama.app.testing.MainDispatcherRule
import com.hakunakuinama.app.testing.testMeal
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Rule
import org.junit.Test

/**
 * The saved-recipes list, and the swipe that removes from it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: MealRepository = mockk()

    private val githeri = testMeal(1, "Githeri", isFavourite = true)
    private val pilau = testMeal(2, "Chicken Pilau", isFavourite = true)

    private val favourites = MutableStateFlow<List<Meal>>(emptyList())

    private fun viewModel() = FavoritesViewModel(
        getFavoriteMeals = GetFavoriteMealsUseCase(repository),
        toggleFavorite = ToggleFavoriteUseCase(repository),
    )

    @org.junit.Before
    fun setUp() {
        every { repository.observeFavourites() } returns favourites
        every { repository.observeIsFavourite(any()) } returns MutableStateFlow(false)
        coEvery { repository.setFavourite(any(), any()) } returns Unit
    }

    @Test
    fun `an empty list is a ready state, not an error`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue("no favourites yet is a normal screen, not a failure", state is FavoritesUiState.Ready)
        assertTrue((state as FavoritesUiState.Ready).isEmpty)
        collector.cancel()
    }

    @Test
    fun `favourites appear as the stream emits`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()

        favourites.value = listOf(githeri, pilau)
        runCurrent()

        val state = viewModel.uiState.value as FavoritesUiState.Ready
        assertEquals(listOf("Githeri", "Chicken Pilau"), state.favorites.map { it.name })
        assertFalse(state.isEmpty)
        collector.cancel()
    }

    @Test
    fun `removing a row announces which meal went`() = runTest(mainDispatcherRule.testDispatcher) {
        favourites.value = listOf(githeri, pilau)
        // Faithful stub: the meal IS a favourite, so the toggle must write false. The
        // generic setUp stub returns false, which would make this a "save" instead.
        every { repository.observeIsFavourite(1L) } returns MutableStateFlow(true)

        val viewModel = viewModel()
        val events = mutableListOf<FavoritesEvent>()
        // onRemove reads uiState, so a collector must exist. In the app the screen always
        // collects it; this mirrors that rather than working around it.
        backgroundScope.launch { viewModel.uiState.collect { } }
        backgroundScope.launch { viewModel.events.collect { events += it } }
        runCurrent()

        viewModel.onRemove(1L)
        runCurrent()

        coVerify { repository.setFavourite(1L, false) }
        assertEquals(listOf(FavoritesEvent.Removed("Githeri")), events)
    }

    @Test
    fun `removing a row that is not in the list does nothing`() = runTest(mainDispatcherRule.testDispatcher) {
        // A stale swipe. Toggling a meal that is not a favourite would silently re-add it.
        favourites.value = listOf(githeri)
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()

        viewModel.onRemove(999L)
        runCurrent()

        coVerify(exactly = 0) { repository.setFavourite(any(), any()) }
    }

    @Test
    fun `removing before anything has been rendered does nothing`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Documents the precondition: with no collection, uiState is still Loading, so
            // there is no row to remove and the action is a safe no-op rather than a
            // toggle that would re-add a non-favourite.
            val viewModel = viewModel()
            viewModel.onRemove(1L)
            runCurrent()
            coVerify(exactly = 0) { repository.setFavourite(any(), any()) }
        }

    @Test
    fun `a failed remove reports the failure instead of claiming success`() =
        runTest(mainDispatcherRule.testDispatcher) {
            favourites.value = listOf(githeri)
            coEvery { repository.setFavourite(any(), any()) } throws IllegalStateException("disk full")

            val viewModel = viewModel()
            val events = mutableListOf<FavoritesEvent>()
            backgroundScope.launch { viewModel.uiState.collect { } }
            backgroundScope.launch { viewModel.events.collect { events += it } }
            runCurrent()

            viewModel.onRemove(1L)
            runCurrent()

            assertEquals(listOf(FavoritesEvent.RemoveFailed), events)
        }

    @Test
    fun `a throwing favourites stream becomes an error state`() = runTest(mainDispatcherRule.testDispatcher) {
        every { repository.observeFavourites() } returns
            flow { throw IllegalStateException("database is corrupt") }

        val viewModel = viewModel()
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        runCurrent()

        assertTrue(viewModel.uiState.value is FavoritesUiState.Error)
        collector.cancel()
    }
}

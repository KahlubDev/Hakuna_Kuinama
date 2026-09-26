package com.hakunakuinama.app.domain.repository

import com.hakunakuinama.app.domain.model.BudgetSummary
import com.hakunakuinama.app.domain.model.GroceryItem
import com.hakunakuinama.app.domain.model.GroceryPlan
import com.hakunakuinama.app.domain.model.Ingredient
import com.hakunakuinama.app.domain.model.Meal
import com.hakunakuinama.app.domain.model.MealMatch
import com.hakunakuinama.app.domain.model.MealSlot
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * The single source of truth for recipes, favourites, the ingredient catalogue and the
 * weekly shopping list. ViewModels talk to this and never to a DAO.
 *
 * Everything is a [Flow] so a change anywhere (favourite toggled in one screen, list
 * regenerated in another) re-renders every observer. Reads never throw for "not found" —
 * they emit `null`/empty, which the UI turns into an empty state.
 */
interface MealRepository {

    // ------------------------------------------------------------------ recipes

    fun observeMeals(): Flow<List<Meal>>

    fun observeMealsForSlot(slot: MealSlot): Flow<List<Meal>>

    fun observeMeal(mealId: Long): Flow<Meal?>

    /**
     * "Today's Suggested Meal" for [slot]: a stable pick that does not change while the
     * user is looking at it, but rotates the next day and the next slot.
     */
    fun observeSuggestedMeal(slot: MealSlot): Flow<Meal?>

    suspend fun getMeal(mealId: Long): Meal?

    // ---------------------------------------------------------------- favourites

    fun observeFavourites(): Flow<List<Meal>>

    fun observeFavouriteIds(): Flow<Set<Long>>

    fun observeIsFavourite(mealId: Long): Flow<Boolean>

    suspend fun setFavourite(mealId: Long, isFavourite: Boolean)

    // ------------------------------------------------------- menu builder inputs

    fun observeIngredients(): Flow<List<Ingredient>>

    /**
     * Scores every recipe against the ticked ingredients. Takes a [Flow] of the selection
     * so the caller can `flatMapLatest` it onto user input without re-subscribing to the DB
     * on every chip tap.
     */
    fun observeMatches(availableIngredientIds: Flow<Set<Long>>): Flow<List<MealMatch>>

    // ------------------------------------------------------------ weekly budget

    fun observeGroceryList(weekStart: LocalDate): Flow<List<GroceryItem>>

    fun observeBudgetSummary(weekStart: LocalDate): Flow<BudgetSummary>

    suspend fun generateGroceryList(plan: GroceryPlan): Result<Int>

    suspend fun addGroceryItem(item: GroceryItem): Result<Unit>

    suspend fun setGroceryItemChecked(id: Long, isChecked: Boolean)

    suspend fun removeGroceryItem(id: Long)

    suspend fun clearGroceryList(weekStart: LocalDate)

    // ------------------------------------------------------------------- seeding

    /** Idempotent; safe to call on every app start. Wired to Room's onCreate callback. */
    suspend fun seedIfNeeded()
}

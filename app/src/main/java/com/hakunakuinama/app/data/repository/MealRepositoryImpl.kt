package com.hakunakuinama.app.data.repository

import com.hakunakuinama.app.data.local.dao.GroceryDao
import com.hakunakuinama.app.data.local.dao.IngredientDao
import com.hakunakuinama.app.data.local.dao.MealDao
import com.hakunakuinama.app.data.local.TransactionRunner
import com.hakunakuinama.app.data.local.entity.GroceryItemEntity
import com.hakunakuinama.app.data.local.seed.DatabaseSeeder
import com.hakunakuinama.app.data.mapper.toDomain
import com.hakunakuinama.app.domain.model.BudgetSummary
import com.hakunakuinama.app.domain.model.FoodCategory
import com.hakunakuinama.app.domain.model.GroceryItem
import com.hakunakuinama.app.domain.model.GroceryPlan
import com.hakunakuinama.app.domain.model.Ingredient
import com.hakunakuinama.app.domain.model.Meal
import com.hakunakuinama.app.domain.model.MealMatch
import com.hakunakuinama.app.domain.model.MealSlot
import com.hakunakuinama.app.domain.model.Weeks
import com.hakunakuinama.app.domain.repository.MealRepository
import com.hakunakuinama.app.domain.util.resultOf
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers

/**
 * Room-backed [MealRepository].
 *
 * Responsibilities kept here on purpose: turning Room rows into domain models, and the two
 * pieces of business logic that are worth unit-testing away from Android —
 * [suggestedMealFor] (which meal to push today) and [mergeIntoLines] (turning selected
 * recipes into one de-duplicated shopping list).
 */
@Singleton
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao,
    private val ingredientDao: IngredientDao,
    private val groceryDao: GroceryDao,
    private val transactions: TransactionRunner,
    private val seeder: DatabaseSeeder,
    /** Injected so tests can pin "today". Phase 2's DI module provides `Clock.systemDefaultZone()`. */
    private val clock: Clock,
) : MealRepository {

    // ------------------------------------------------------------------ recipes

    override fun observeMeals(): Flow<List<Meal>> =
        mealDao.observeAllWithDetails()
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)

    override fun observeMealsForSlot(slot: MealSlot): Flow<List<Meal>> =
        mealDao.observeBySlotWithDetails(slot)
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)

    override fun observeMeal(mealId: Long): Flow<Meal?> =
        mealDao.observeByIdWithDetails(mealId)
            .map { it?.toDomain() }
            .flowOn(Dispatchers.Default)

    override suspend fun getMeal(mealId: Long): Meal? =
        mealDao.getByIdWithDetails(mealId)?.toDomain()

    override fun observeSuggestedMeal(slot: MealSlot): Flow<Meal?> =
        observeMealsForSlot(slot)
            .map { meals -> suggestedMealFor(meals, slot, LocalDate.now(clock)) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)

    // ---------------------------------------------------------------- favourites

    override fun observeFavourites(): Flow<List<Meal>> =
        mealDao.observeFavouritesWithDetails()
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)

    override fun observeFavouriteIds(): Flow<Set<Long>> =
        mealDao.observeFavouriteIds()
            .map { it.toSet() }
            .flowOn(Dispatchers.Default)

    override fun observeIsFavourite(mealId: Long): Flow<Boolean> =
        mealDao.observeIsFavourite(mealId)
            .map { it ?: false }
            .flowOn(Dispatchers.Default)

    override suspend fun setFavourite(mealId: Long, isFavourite: Boolean) {
        val timestamp = if (isFavourite) System.currentTimeMillis() else null
        mealDao.setFavourite(mealId, isFavourite, timestamp)
    }

    // ------------------------------------------------------- menu builder inputs

    override fun observeIngredients(): Flow<List<Ingredient>> =
        ingredientDao.observeAll()
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)

    override fun observeMatches(availableIngredientIds: Flow<Set<Long>>): Flow<List<MealMatch>> =
        combine(observeMeals(), availableIngredientIds) { meals, available ->
            meals.map { meal -> MealMatch.of(meal, available) }
                .filter { it.matchPercentage >= MIN_MATCH_PERCENT }
                // Best matches first; among equals, the cheapest plate wins. This is the
                // "no sleeping on food" promise: the suggestion you can actually afford.
                .sortedWith(
                    compareByDescending<MealMatch> { it.canCookNow }
                        .thenByDescending { it.matchPercentage }
                        .thenBy { it.meal.costPerServingKes }
                        .thenBy { it.meal.name },
                )
        }.flowOn(Dispatchers.Default)

    // ------------------------------------------------------------ weekly budget

    override fun observeGroceryList(weekStart: LocalDate): Flow<List<GroceryItem>> =
        groceryDao.observeForWeek(Weeks.startOf(weekStart).toEpochDay())
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)

    override fun observeBudgetSummary(weekStart: LocalDate): Flow<BudgetSummary> {
        val week = Weeks.startOf(weekStart).toEpochDay()
        return combine(
            groceryDao.observePlannedKes(week),
            groceryDao.observeSpentKes(week),
            groceryDao.observeForWeek(week),
        ) { planned, spent, items ->
            BudgetSummary(
                plannedKes = planned,
                spentKes = spent,
                outstandingKes = (planned - spent).coerceAtLeast(0.0),
                lineCount = items.size,
                checkedCount = items.count { it.isChecked },
            )
        }.flowOn(Dispatchers.Default)
    }

    override suspend fun generateGroceryList(plan: GroceryPlan): Result<Int> = resultOf {
        require(plan.mealIds.isNotEmpty()) { "Pick at least one meal first" }
        require(plan.servingsPerMeal > 0) { "servingsPerMeal must be >= 1, was ${plan.servingsPerMeal}" }
        val week = Weeks.startOf(plan.weekStart).toEpochDay()
        val meals = mealDao.getByIdsWithDetails(plan.mealIds).map { it.toDomain() }
        check(meals.isNotEmpty()) { "Cannot build a shopping list: none of ${plan.mealIds} exist" }

        val lines = mergeIntoLines(meals, plan.servingsPerMeal)

        // One transaction: a half-written shopping list is worse than no list at all.
        transactions.run {
            val rows = if (plan.replaceExisting) {
                groceryDao.clearWeek(week)
                lines.values.map { it.toEntity(week) }
            } else {
                // Top-up mode: fold the new quantities into the lines the user has not bought
                // yet, and leave the already-ticked (paid) lines exactly as they are.
                val current = groceryDao.getForWeek(week)
                groceryDao.clearWeek(week)
                mergeRows(current, lines, week)
            }
            groceryDao.insertAll(rows)
            rows.size
        }
    }

    override suspend fun addGroceryItem(item: GroceryItem): Result<Unit> = resultOf {
        groceryDao.insertAll(listOf(item.toEntity()))
        Unit
    }

    override suspend fun setGroceryItemChecked(id: Long, isChecked: Boolean) {
        groceryDao.setChecked(id, isChecked)
    }

    override suspend fun removeGroceryItem(id: Long) {
        groceryDao.deleteById(id)
    }

    override suspend fun clearGroceryList(weekStart: LocalDate) {
        groceryDao.clearWeek(Weeks.startOf(weekStart).toEpochDay())
    }

    // ------------------------------------------------------------------- seeding

    override suspend fun seedIfNeeded() {
        seeder.seedIfEmpty()
    }

    private companion object {
        /** Below this, a "match" is a coincidence, not a suggestion. */
        const val MIN_MATCH_PERCENT = 25
    }
}

// ----------------------------------------------------------------- business logic

/**
 * Picks the meal to push today. Deterministic per (day, slot) so the dashboard does not
 * reshuffle on every recomposition or every app resume, but the suggestion still rotates
 * day to day and between breakfast/lunch/dinner.
 */
internal fun suggestedMealFor(meals: List<Meal>, slot: MealSlot, today: LocalDate): Meal? {
    if (meals.isEmpty()) return null
    val index = Math.floorMod(today.toEpochDay() + slot.ordinal, meals.size)
    return meals[index]
}

/**
 * Flattens the selected recipes' shoppable ingredients into one de-duplicated list.
 *
 * Merging is by name + unit, so "0.5 kg maize flour" from Ugali and "0.5 kg maize flour"
 * from Githeri become a single 1 kg line. Optional and pantry ingredients are dropped:
 * a shopping list should not contain salt.
 *
 * TODO(servings-scaling): [servingsMultiplier] is applied to whole batches, not to
 * people. Every recipe is written for 2 or 3 servings (`Meal.servings`), so asking for
 * 4 portions today buys 4 batches of each recipe — 8 to 12 portions. It is inert today
 * because [com.hakunakuinama.app.ui.viewmodel.GroceryListViewModel.onGenerateForRecipe]
 * is only ever called with the default of 1, and no servings control exists in the UI;
 * it becomes a visible over-buy the moment a scaler ships. The fix is to divide by
 * `meal.servings` rather than multiply by 1.
 *
 * This is the same feature as the TODO on
 * [com.hakunakuinama.app.domain.model.MealStep.instruction]: both are "scale a recipe to
 * N people", and they should be built together. A servings control that corrects the
 * shopping list but leaves the prose reading "add 500 g maize flour" hands the user a
 * number they cannot act on. Fix the batch arithmetic and the step quantities in one
 * change, or neither.
 */
internal fun mergeIntoLines(meals: List<Meal>, servingsMultiplier: Int): Map<String, PendingLine> {
    val lines = linkedMapOf<String, PendingLine>()
    meals.forEach { meal ->
        meal.shoppableIngredients.forEach { usage ->
            // Same key format as GroceryItemEntity.mergeKey, so a re-generated week lines up
            // with what is already on the list.
            val key = "${usage.ingredient.name}|${usage.unit}"
            val line = lines.getOrPut(key) {
                PendingLine(
                    name = usage.ingredient.name,
                    emoji = usage.ingredient.emoji,
                    category = usage.ingredient.category,
                    unit = usage.unit,
                    unitPriceKes = usage.ingredient.pricePerUnitKes,
                )
            }
            line.quantity += usage.quantity * servingsMultiplier
            line.sourceMealIds += meal.id
        }
    }
    return lines
}

/**
 * A shopping line that exists only in memory until it is inserted.
 *
 * [quantity] and [sourceMealIds] are `var` because this is an accumulator: two recipes
 * each needing 0.5 kg of maize flour must fold into one 1 kg line, and a `val` cannot be
 * reassigned. Once it leaves this function it is an immutable value again.
 */
internal data class PendingLine(
    val name: String,
    val emoji: String,
    val category: FoodCategory,
    val unit: String,
    val unitPriceKes: Double,
    var quantity: Double = 0.0,
    var sourceMealIds: Set<Long> = emptySet(),
)

// ------------------------------------------------------------------- helpers

private fun mergeRows(
    existing: List<GroceryItemEntity>,
    generated: Map<String, PendingLine>,
    weekStartEpochDay: Long,
): List<GroceryItemEntity> {
    val rows = mutableListOf<GroceryItemEntity>()

    // Already-bought lines pass through untouched.
    existing.filter { it.isChecked }.forEach { rows += it }

    val remaining = mutableSetOf<String>()
    existing.filterNot { it.isChecked }.forEach { row ->
        val extra = generated[row.mergeKey]
        remaining += row.mergeKey
        rows += if (extra == null) {
            row
        } else {
            row.copy(
                quantity = row.quantity + extra.quantity,
                sourceMealIds = (row.sourceMealIds + extra.sourceMealIds).distinct(),
            )
        }
    }

    generated.filterKeys { it !in remaining }.values.forEach { line ->
        rows += line.toEntity(weekStartEpochDay)
    }
    return rows
}

private fun PendingLine.toEntity(weekStartEpochDay: Long) = GroceryItemEntity(
    name = name,
    emoji = emoji,
    category = category,
    quantity = quantity,
    unit = unit,
    unitPriceKes = unitPriceKes,
    isChecked = false,
    sourceMealIds = sourceMealIds.toList(),
    weekStartEpochDay = weekStartEpochDay,
)

private fun GroceryItem.toEntity() = GroceryItemEntity(
    id = id,
    name = name,
    emoji = emoji,
    category = category,
    quantity = quantity,
    unit = unit,
    unitPriceKes = unitPriceKes,
    isChecked = isChecked,
    sourceMealIds = sourceMealIds,
    weekStartEpochDay = weekStartEpochDay,
)

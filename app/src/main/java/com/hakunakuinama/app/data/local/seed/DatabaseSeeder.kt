package com.hakunakuinama.app.data.local.seed

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hakunakuinama.app.data.local.HakunaKuinamaDatabase
import com.hakunakuinama.app.data.local.entity.IngredientEntity
import com.hakunakuinama.app.data.local.entity.MealEntity
import com.hakunakuinama.app.data.local.entity.MealIngredientCrossRef
import com.hakunakuinama.app.data.local.entity.MealStepEntity
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Writes [SeedData] into the database exactly once per installation.
 *
 * Why a [Provider] of the database instead of the database itself: the seeder has to be
 * handed to Room's builder *while* that builder creates the database, so a direct
 * constructor dependency would be a cycle. `Provider` defers the lookup.
 */
class DatabaseSeeder @Inject constructor(
    private val database: Provider<HakunaKuinamaDatabase>,
) {

    /**
     * Seeds the catalogue if the `meals` table is still empty, inside a single
     * transaction so a crash mid-seed can never leave a half-populated app.
     */
    suspend fun seedIfEmpty(): SeedResult = withContext(Dispatchers.IO) {
        val db = database.get()
        val mealDao = db.mealDao()
        val ingredientDao = db.ingredientDao()

        db.withTransaction {
            val existing = mealDao.count()
            if (existing > 0) {
                return@withTransaction SeedResult.Skipped(existing)
            }

            // Catalogue first: recipe usages reference ingredients by name and need their ids.
            val insertedIngredients = ingredientDao.insertAll(SeedData.ingredients.map(IngredientSeed::toEntity))
            check(insertedIngredients.size == SeedData.ingredients.size) {
                "Seed ignored ${SeedData.ingredients.size - insertedIngredients.size} ingredient(s) — duplicate names in SeedData?"
            }
            val idsByName = ingredientDao.getAll().associate { it.name to it.id }

            val newIds = mealDao.insertMeals(SeedData.meals.map(MealSeed::toEntity))
            check(newIds.size == SeedData.meals.size) {
                "Seed insert ignored rows for ${SeedData.meals.size - newIds.size} meal(s) — duplicate meal names?"
            }

            val stepEntities = mutableListOf<MealStepEntity>()
            val usageEntities = mutableListOf<MealIngredientCrossRef>()
            SeedData.meals.forEachIndexed { index, seed ->
                val mealId = newIds[index]
                seed.steps.forEachIndexed { stepIndex, step ->
                    stepEntities += MealStepEntity(
                        mealId = mealId,
                        stepNumber = stepIndex + 1,
                        instruction = step.instruction,
                        durationMinutes = step.durationMinutes,
                    )
                }
                seed.ingredients.forEach { usage ->
                    val ingredientId = requireNotNull(idsByName[usage.ingredient]) {
                        "SeedData references unknown ingredient '${usage.ingredient}' in '${seed.name}'"
                    }
                    usageEntities += MealIngredientCrossRef(
                        mealId = mealId,
                        ingredientId = ingredientId,
                        quantity = usage.quantity,
                        unit = usage.unit,
                        isOptional = usage.isOptional,
                        mustBuy = usage.mustBuy,
                    )
                }
            }

            mealDao.insertSteps(stepEntities)
            mealDao.insertIngredientUsages(usageEntities)

            SeedResult.Seeded(
                meals = newIds.size,
                ingredients = idsByName.size,
            )
        }
    }
}

/**
 * Triggers seeding on first database creation.
 *
 * The seed is launched on [applicationScope] instead of run inline: `onCreate` is invoked
 * *inside* Room's creation transaction, and issuing DAO writes from that thread before the
 * transaction commits deadlocks. `onOpen` is intentionally not used — the app reads the DB
 * long before it is opened, and `seedIfEmpty()` is idempotent anyway.
 */
class SeedDatabaseCallback(
    private val applicationScope: CoroutineScope,
    private val seeder: DatabaseSeeder,
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        applicationScope.launch(Dispatchers.IO) { seeder.seedIfEmpty() }
    }
}

sealed interface SeedResult {
    data class Seeded(val meals: Int, val ingredients: Int) : SeedResult
    data class Skipped(val existingMeals: Int) : SeedResult
}

// ------------------------------------------------------------------ seed -> entity

private fun IngredientSeed.toEntity() = IngredientEntity(
    name = name,
    category = category,
    unit = unit,
    pricePerUnitKes = pricePerUnitKes,
    emoji = emoji,
    isStaple = isStaple,
    defaultQuantity = defaultQuantity,
)

private fun MealSeed.toEntity() = MealEntity(
    name = name,
    tagline = tagline,
    emoji = emoji,
    slot = slot,
    difficulty = difficulty,
    prepMinutes = prepMinutes,
    cookMinutes = cookMinutes,
    servings = servings,
    tags = tags,
)

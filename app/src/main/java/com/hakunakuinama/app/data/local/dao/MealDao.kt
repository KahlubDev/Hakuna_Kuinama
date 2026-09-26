package com.hakunakuinama.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.hakunakuinama.app.data.local.entity.MealEntity
import com.hakunakuinama.app.data.local.entity.MealIngredientCrossRef
import com.hakunakuinama.app.data.local.entity.MealStepEntity
import com.hakunakuinama.app.data.local.relation.MealWithDetails
import com.hakunakuinama.app.domain.model.MealSlot
import kotlinx.coroutines.flow.Flow

/**
 * Recipe reads and writes.
 *
 * Reads return [MealWithDetails] (recipe + steps + ingredient usages) and are marked
 * `@Transaction` so Room runs them as one consistent graph fetch instead of N queries.
 */
@Dao
interface MealDao {

    // ---------------------------------------------------------------- reads

    @Transaction
    @Query("SELECT * FROM meals ORDER BY slot ASC, name ASC")
    fun observeAllWithDetails(): Flow<List<MealWithDetails>>

    @Transaction
    @Query("SELECT * FROM meals WHERE id = :mealId")
    fun observeByIdWithDetails(mealId: Long): Flow<MealWithDetails?>

    @Transaction
    @Query("SELECT * FROM meals WHERE slot = :slot ORDER BY name ASC")
    fun observeBySlotWithDetails(slot: MealSlot): Flow<List<MealWithDetails>>

    @Transaction
    @Query("SELECT * FROM meals WHERE isFavourite = 1 ORDER BY favouritedAt DESC, name ASC")
    fun observeFavouritesWithDetails(): Flow<List<MealWithDetails>>

    @Transaction
    @Query(
        """
        SELECT * FROM meals
        WHERE name LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY name ASC
        """,
    )
    fun search(query: String): Flow<List<MealWithDetails>>

    @Transaction
    @Query("SELECT * FROM meals WHERE id = :mealId")
    suspend fun getByIdWithDetails(mealId: Long): MealWithDetails?

    @Transaction
    @Query("SELECT * FROM meals WHERE id IN (:mealIds)")
    suspend fun getByIdsWithDetails(mealIds: List<Long>): List<MealWithDetails>

    @Query("SELECT isFavourite FROM meals WHERE id = :mealId")
    fun observeIsFavourite(mealId: Long): Flow<Boolean?>

    /** Lightweight ids only — the Menu Builder needs this, not whole recipes. */
    @Query("SELECT id FROM meals WHERE isFavourite = 1 ORDER BY favouritedAt DESC")
    fun observeFavouriteIds(): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM meals")
    suspend fun count(): Int

    // --------------------------------------------------------------- writes

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMeals(meals: List<MealEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSteps(steps: List<MealStepEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIngredientUsages(usages: List<MealIngredientCrossRef>)

    @Update
    suspend fun updateMeal(meal: MealEntity)

    @Query("UPDATE meals SET isFavourite = :isFavourite, favouritedAt = :favouritedAt WHERE id = :mealId")
    suspend fun setFavourite(mealId: Long, isFavourite: Boolean, favouritedAt: Long?)

    @Query("DELETE FROM meals WHERE id = :mealId")
    suspend fun deleteById(mealId: Long)
}

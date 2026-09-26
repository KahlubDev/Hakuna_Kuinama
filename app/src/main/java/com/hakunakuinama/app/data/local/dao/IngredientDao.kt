package com.hakunakuinama.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hakunakuinama.app.data.local.entity.IngredientEntity
import com.hakunakuinama.app.domain.model.FoodCategory
import kotlinx.coroutines.flow.Flow

/** The ingredient catalogue shown as chips in the Menu Builder. */
@Dao
interface IngredientDao {

    /** Staples first, then alphabetically — the exact order the picker renders. */
    @Query(
        """
        SELECT * FROM ingredients
        ORDER BY isStaple DESC, name ASC
        """,
    )
    fun observeAll(): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE category = :category ORDER BY isStaple DESC, name ASC")
    fun observeByCategory(category: FoodCategory): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE id IN (:ids)")
    fun observeByIds(ids: List<Long>): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients")
    suspend fun getAll(): List<IngredientEntity>

    @Query("SELECT * FROM ingredients WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): IngredientEntity?

    @Query("SELECT COUNT(*) FROM ingredients")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(ingredients: List<IngredientEntity>): List<Long>
}

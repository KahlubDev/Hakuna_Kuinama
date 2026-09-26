package com.hakunakuinama.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hakunakuinama.app.data.local.entity.GroceryItemEntity
import kotlinx.coroutines.flow.Flow

/** The weekly shopping list. All money maths is done in SQL, not in Kotlin. */
@Dao
interface GroceryDao {

    @Query(
        """
        SELECT * FROM grocery_items
        WHERE weekStartEpochDay = :weekStartEpochDay
        ORDER BY category ASC, name ASC
        """,
    )
    fun observeForWeek(weekStartEpochDay: Long): Flow<List<GroceryItemEntity>>

    @Query("SELECT * FROM grocery_items WHERE id = :id")
    suspend fun getById(id: Long): GroceryItemEntity?

    @Query("SELECT * FROM grocery_items WHERE weekStartEpochDay = :weekStartEpochDay")
    suspend fun getForWeek(weekStartEpochDay: Long): List<GroceryItemEntity>

    /**
     * Upsert on the natural key (week + name|unit) so regenerating a list twice does not
     * duplicate lines — quantities are merged by the repository before this is called.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<GroceryItemEntity>): List<Long>

    @Update
    suspend fun update(item: GroceryItemEntity)

    @Query("UPDATE grocery_items SET isChecked = :isChecked WHERE id = :id")
    suspend fun setChecked(id: Long, isChecked: Boolean)

    @Query("UPDATE grocery_items SET isChecked = 0 WHERE weekStartEpochDay = :weekStartEpochDay")
    suspend fun uncheckAll(weekStartEpochDay: Long)

    @Query("DELETE FROM grocery_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM grocery_items WHERE weekStartEpochDay = :weekStartEpochDay")
    suspend fun clearWeek(weekStartEpochDay: Long)

    /** Money left to spend this week (unchecked lines only). */
    @Query(
        """
        SELECT COALESCE(SUM(quantity * unitPriceKes), 0.0) FROM grocery_items
        WHERE weekStartEpochDay = :weekStartEpochDay AND isChecked = 0
        """,
    )
    fun observeOutstandingKes(weekStartEpochDay: Long): Flow<Double>

    /** Full budget of the list regardless of what has been ticked off. */
    @Query(
        """
        SELECT COALESCE(SUM(quantity * unitPriceKes), 0.0) FROM grocery_items
        WHERE weekStartEpochDay = :weekStartEpochDay
        """,
    )
    fun observePlannedKes(weekStartEpochDay: Long): Flow<Double>

    /** Money already spent = planned minus outstanding. */
    @Query(
        """
        SELECT COALESCE(SUM(quantity * unitPriceKes), 0.0) FROM grocery_items
        WHERE weekStartEpochDay = :weekStartEpochDay AND isChecked = 1
        """,
    )
    fun observeSpentKes(weekStartEpochDay: Long): Flow<Double>
}

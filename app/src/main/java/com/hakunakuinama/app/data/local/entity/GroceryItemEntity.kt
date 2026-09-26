package com.hakunakuinama.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hakunakuinama.app.domain.model.FoodCategory

/**
 * A line on the weekly shopping list.
 *
 * Lines are merged on the Kotlin-side natural key "name|unit" (see [mergeKey]), so
 * "0.5 kg maize flour" from Ugali plus "0.5 kg maize flour" from Githeri becomes one
 * 1 kg line. That key is deliberately NOT a column: Room cannot index a computed
 * property, and no unique constraint is needed because the repository always rewrites
 * a week inside a single transaction (read → merge → clear → insert).
 *
 * @param weekStartEpochDay [java.time.LocalDate.toEpochDay] of that week's Monday.
 * @param sourceMealIds Pipe-delimited meal ids ("3|7") that put this line on the list.
 */
@Entity(
    tableName = "grocery_items",
    indices = [Index("weekStartEpochDay")],
)
data class GroceryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val emoji: String,
    val category: FoodCategory,
    val quantity: Double,
    val unit: String,
    val unitPriceKes: Double,
    @ColumnInfo(defaultValue = "0")
    val isChecked: Boolean = false,
    @ColumnInfo(defaultValue = "")
    val sourceMealIds: List<Long> = emptyList(),
    val weekStartEpochDay: Long,
) {
    /** Kotlin-side natural key only — not a database column. See the class doc. */
    val mergeKey: String get() = "$name|$unit"

    val estimatedCostKes: Double get() = quantity * unitPriceKes
}

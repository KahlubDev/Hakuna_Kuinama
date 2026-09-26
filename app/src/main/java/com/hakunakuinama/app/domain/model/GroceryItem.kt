package com.hakunakuinama.app.domain.model

import java.time.LocalDate

/**
 * A line on the weekly shopping list.
 *
 * Lines are merged by (name, unit) so picking Ugali & Sukuma *and* Githeri only lists
 * maize flour once, with the quantities added together.
 *
 * @param weekStartEpochDay [LocalDate.toEpochDay] of the Monday that starts the week.
 * @param sourceMealIds Meals that contributed to this line (drives the "why is this here?" UI).
 */
data class GroceryItem(
    val id: Long,
    val name: String,
    val emoji: String,
    val category: FoodCategory,
    val quantity: Double,
    val unit: String,
    val unitPriceKes: Double,
    val isChecked: Boolean,
    val sourceMealIds: List<Long>,
    val weekStartEpochDay: Long,
) {
    val estimatedCostKes: Double get() = quantity * unitPriceKes

    /** Money still to spend on this line (checked-off lines are already paid). */
    val outstandingCostKes: Double get() = if (isChecked) 0.0 else estimatedCostKes

    fun belongsTo(week: LocalDate): Boolean = weekStartEpochDay == week.toEpochDay()
}

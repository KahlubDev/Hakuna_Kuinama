package com.hakunakuinama.app.domain.model

import java.time.LocalDate

/**
 * What the user picked on the Budget Tracker screen before asking for a shopping list.
 *
 * @param servingsPerMeal Multiplier applied to every recipe quantity, so "4 people" is a
 *        slider rather than a second code path.
 * @param replaceExisting True = rebuild the week from scratch (the UI confirms this, because
 *        it drops ticked-off items). False = top up the existing unchecked lines.
 */
data class GroceryPlan(
    val mealIds: List<Long>,
    val weekStart: LocalDate = Weeks.current(),
    val servingsPerMeal: Int = 1,
    val replaceExisting: Boolean = true,
)

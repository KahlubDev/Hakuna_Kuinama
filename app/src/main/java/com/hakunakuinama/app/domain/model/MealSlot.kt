package com.hakunakuinama.app.domain.model

/**
 * Meal time slot. Drives the Dashboard "Today's Suggested Meal" pick.
 * [fromHour] is the single source of truth for time-of-day classification so the
 * Dashboard, notifications and the weekly budget all agree on what "lunch" means.
 */
enum class MealSlot(val label: String, val startHour: Int) {
    BREAKFAST("Breakfast", 5),
    LUNCH("Lunch", 11),
    DINNER("Dinner", 16),
    SNACK("Snack", 22),
    ;

    companion object {
        fun fromHour(hour: Int): MealSlot = when (hour) {
            in 5..10 -> BREAKFAST
            in 11..15 -> LUNCH
            in 16..21 -> DINNER
            else -> SNACK // 00:00–04:59 late night, 22:00–23:59 proper snack
        }
    }
}

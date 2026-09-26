package com.hakunakuinama.app.domain.model

/**
 * Meal time slot. Drives the Dashboard "Today's Suggested Meal" pick.
 *
 * [fromHour] is the single source of truth for time-of-day classification so the
 * Dashboard, notifications and the weekly budget all agree on what "lunch" means.
 *
 * No display label lives here on purpose: user-facing copy belongs in `strings.xml`, and
 * the UI maps the enum to a resource (`ui/util/Labels.kt`). A bilingual build must not
 * require editing the domain layer.
 */
enum class MealSlot(val startHour: Int) {
    BREAKFAST(5),
    LUNCH(11),
    DINNER(16),
    SNACK(22),
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

package com.hakunakuinama.app.domain.model

/** Money view of the current week's shopping list, in KES. */
data class BudgetSummary(
    val plannedKes: Double = 0.0,
    val spentKes: Double = 0.0,
    val outstandingKes: Double = 0.0,
    val lineCount: Int = 0,
    val checkedCount: Int = 0,
) {
    val isEmpty: Boolean get() = lineCount == 0

    /** 0..1, how much of the planned budget has been ticked off. */
    val progress: Float
        get() = if (plannedKes <= 0.0) 0f else (spentKes / plannedKes).coerceIn(0.0, 1.0).toFloat()

    val remainingCount: Int get() = (lineCount - checkedCount).coerceAtLeast(0)
}

package com.hakunakuinama.app.domain.model

/**
 * Result of testing one recipe against the ingredients the user ticked in the Menu Builder.
 *
 * @param matchPercentage 0..100 — share of *required* ingredients already in the kitchen.
 * @param missingIngredients Required items the user still has to buy, cheapest first
 *        (so the UI can show "you only miss 2 cheap things").
 */
data class MealMatch(
    val meal: Meal,
    val matchedIngredientIds: Set<Long>,
    val missingIngredients: List<MealIngredient>,
    val matchPercentage: Int,
) {
    /** Everything required is on hand — "Cook now". */
    val canCookNow: Boolean get() = missingIngredients.isEmpty()

    val missingCostKes: Double get() = missingIngredients.sumOf { it.estimatedCostKes }

    companion object {
        /**
         * Scores [meal] against [availableIngredientIds].
         *
         * Required means [Meal.shoppableIngredients]: neither an optional extra nor a
         * pantry staple, so a recipe is judged entirely on the things the user would
         * actually have to buy. Ticking an optional extra never raises the score, and a
         * staple the kitchen is assumed to own is never scored as missing.
         */
        fun of(meal: Meal, availableIngredientIds: Set<Long>): MealMatch {
            val required = meal.shoppableIngredients
            if (required.isEmpty()) {
                return MealMatch(meal, emptySet(), emptyList(), matchPercentage = 100)
            }

            val matched = required.filter { it.ingredient.id in availableIngredientIds }
            val missing = required
                .filterNot { it.ingredient.id in availableIngredientIds }
                .sortedBy { it.estimatedCostKes }

            val percentage = ((matched.size * 100) / required.size).coerceIn(0, 100)
            return MealMatch(
                meal = meal,
                matchedIngredientIds = matched.map { it.ingredient.id }.toSet(),
                missingIngredients = missing,
                matchPercentage = percentage,
            )
        }
    }
}

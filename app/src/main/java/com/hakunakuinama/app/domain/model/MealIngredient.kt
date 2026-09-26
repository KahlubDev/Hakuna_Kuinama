package com.hakunakuinama.app.domain.model

/** One ordered instruction of a recipe. */
data class MealStep(
    val number: Int,
    val instruction: String,
    val durationMinutes: Int? = null,
)

/**
 * An ingredient as it is used by one specific meal.
 *
 * @param isOptional Optional items never block a match and are not added to the
 *        grocery list (e.g. coriander in pilau).
 * @param mustBuy False for pantry items assumed to be on hand (salt, water, oil).
 */
data class MealIngredient(
    val ingredient: Ingredient,
    val quantity: Double,
    val unit: String,
    val isOptional: Boolean,
    val mustBuy: Boolean,
) {
    val estimatedCostKes: Double
        get() = if (mustBuy) ingredient.costFor(quantity) else 0.0
}

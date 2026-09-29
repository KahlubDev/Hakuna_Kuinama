package com.hakunakuinama.app.domain.model

/**
 * One ordered instruction of a recipe.
 *
 * @param instruction Prose that may embed quantities — "add 500 g maize flour". Those
 *        numbers are written by hand in the seed catalogue and are not derived from
 *        [MealIngredient.quantity], so the two can disagree and there is nothing that
 *        stops them drifting apart as the catalogue is edited.
 *
 * TODO(servings-scaling): this is the second half of the same feature as the
 * [mergeIntoLines] TODO, and neither half should ship alone. Quantities in
 * [instruction] cannot respond to a servings change, so a user who scales a recipe to
 * four people gets a shopping list that has been corrected to four portions and a method
 * that still says "500 g" — a number that contradicts the list above it. Scaling a recipe
 * properly means the step quantities have to come from somewhere structured
 * (an amount plus a unit per step, resolved against the chosen servings) instead of being
 * prose. Until then, a servings control is a half-feature.
 */
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

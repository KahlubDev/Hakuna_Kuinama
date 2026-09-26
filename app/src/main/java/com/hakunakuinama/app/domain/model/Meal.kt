package com.hakunakuinama.app.domain.model

/**
 * A full recipe. Assembled by the data layer from the Room graph:
 * meals + steps + (ingredients ⋈ usage).
 */
data class Meal(
    val id: Long,
    val name: String,
    val tagline: String,
    val emoji: String,
    val imageUrl: String? = null,
    val slot: MealSlot,
    val difficulty: MealDifficulty,
    val prepMinutes: Int,
    val cookMinutes: Int,
    val servings: Int,
    val tags: Set<String> = emptySet(),
    val steps: List<MealStep> = emptyList(),
    val ingredients: List<MealIngredient> = emptyList(),
    /**
     * Whether the user has saved this recipe.
     *
     * Carried on the recipe rather than fetched separately so the favourite state travels
     * in the row the screen is already observing: toggling it re-emits the recipe stream
     * and the heart updates, with no second query and no risk of the flag and the card
     * disagreeing.
     */
    val isFavourite: Boolean = false,
) {
    val totalMinutes: Int get() = prepMinutes + cookMinutes

    /** Cost of the whole batch in KES. */
    val batchCostKes: Double
        get() = ingredients.sumOf { it.estimatedCostKes }

    /** Cost of a single plate in KES — the number the UI puts in the BudgetBadge. */
    val costPerServingKes: Double
        get() = if (servings <= 0) 0.0 else batchCostKes / servings

    /**
     * What the shopper actually has to buy: required, non-pantry ingredients.
     *
     * Optional extras are deliberately excluded so the weekly budget can never be
     * inflated by a "maybe" (2 boiled eggs on the side of ugali?). The recipe screen
     * surfaces them separately via [optionalExtras] so they are still one tap away.
     */
    val shoppableIngredients: List<MealIngredient>
        get() = ingredients.filter { it.mustBuy && !it.isOptional }

    /** Nice-to-have additions, priced but never added to the grocery list. */
    val optionalExtras: List<MealIngredient>
        get() = ingredients.filter { it.isOptional && it.mustBuy }
}

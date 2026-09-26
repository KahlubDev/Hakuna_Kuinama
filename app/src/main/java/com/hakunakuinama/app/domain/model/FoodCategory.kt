package com.hakunakuinama.app.domain.model

/**
 * Shared by [Ingredient] and [GroceryItem] so the Menu Builder and the weekly
 * shopping list can be grouped with the same taxonomy.
 *
 * `OTHER` is for lines a user adds themselves; nothing in the seeded catalogue uses it.
 */
enum class FoodCategory {
    PRODUCE,
    PROTEIN,
    STAPLE,
    DAIRY,
    SPICES,
    OILS,
    OTHER,
}

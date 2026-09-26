package com.hakunakuinama.app.domain.model

/**
 * Shared by [Ingredient] and [GroceryItem] so the Menu Builder and the weekly
 * shopping list can be grouped with the same taxonomy.
 */
enum class FoodCategory(val label: String) {
    PRODUCE("Veggies & Fruits"),
    PROTEIN("Proteins"),
    STAPLE("Staples & Flour"),
    DAIRY("Dairy"),
    SPICES("Spices"),
    OILS("Oils & Fats"),
    OTHER("Other"),
}

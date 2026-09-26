package com.hakunakuinama.app.domain.model

/**
 * A single item in the shared ingredient catalogue.
 *
 * @param unit The *purchase* unit (e.g. "1 kg bag", "bunch", "tray of 30").
 * @param pricePerUnitKes Retail price in KES for one [unit], used to price the grocery list.
 * @param isStaple True for items a Kenyan bachelor almost always has/buys cheaply.
 *        Staples are shown first in the Menu Builder picker.
 */
data class Ingredient(
    val id: Long,
    val name: String,
    val category: FoodCategory,
    val unit: String,
    val pricePerUnitKes: Double,
    val emoji: String,
    val isStaple: Boolean,
) {
    /** Cost in KES of [quantity] of this ingredient, e.g. 0.5 kg maize flour @ 120/kg = 60. */
    fun costFor(quantity: Double): Double = quantity * pricePerUnitKes
}

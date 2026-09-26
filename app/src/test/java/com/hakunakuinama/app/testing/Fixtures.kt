package com.hakunakuinama.app.testing

import com.hakunakuinama.app.domain.model.FoodCategory
import com.hakunakuinama.app.domain.model.Ingredient
import com.hakunakuinama.app.domain.model.Meal
import com.hakunakuinama.app.domain.model.MealDifficulty
import com.hakunakuinama.app.domain.model.MealIngredient
import com.hakunakuinama.app.domain.model.MealSlot
import com.hakunakuinama.app.domain.model.MealStep

/**
 * Builders for the domain types the tests need.
 *
 * Defaults are chosen so a test only states what it actually cares about. Note the price
 * convention: 100 KES per unit with a default quantity of 0.5, so a two-ingredient,
 * two-serving recipe costs 50 KES per plate — arithmetic a test can do in its head.
 */
fun testIngredient(
    id: Long,
    name: String = "Ingredient $id",
    pricePerUnitKes: Double = 100.0,
    isStaple: Boolean = false,
) = Ingredient(
    id = id,
    name = name,
    category = FoodCategory.STAPLE,
    unit = "kg",
    pricePerUnitKes = pricePerUnitKes,
    emoji = "🌽",
    isStaple = isStaple,
)

fun testMealIngredient(
    ingredientId: Long,
    name: String = "Ingredient $ingredientId",
    pricePerUnitKes: Double = 100.0,
    quantity: Double = 0.5,
    isOptional: Boolean = false,
    mustBuy: Boolean = true,
) = MealIngredient(
    ingredient = testIngredient(ingredientId, name, pricePerUnitKes),
    quantity = quantity,
    unit = "kg",
    isOptional = isOptional,
    mustBuy = mustBuy,
)

fun testMeal(
    id: Long,
    name: String = "Meal $id",
    slot: MealSlot = MealSlot.LUNCH,
    ingredientIds: List<Long> = emptyList(),
    servings: Int = 2,
    isFavourite: Boolean = false,
    steps: List<MealStep> = listOf(MealStep(1, "Cook it")),
) = Meal(
    id = id,
    name = name,
    tagline = "Tagline for $name",
    emoji = "🍲",
    slot = slot,
    difficulty = MealDifficulty.EASY,
    prepMinutes = 5,
    cookMinutes = 10,
    servings = servings,
    steps = steps,
    ingredients = ingredientIds.map { testMealIngredient(it) },
    isFavourite = isFavourite,
)

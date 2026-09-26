package com.hakunakuinama.app.data.mapper

import com.hakunakuinama.app.data.local.entity.GroceryItemEntity
import com.hakunakuinama.app.data.local.entity.IngredientEntity
import com.hakunakuinama.app.data.local.entity.MealStepEntity
import com.hakunakuinama.app.data.local.relation.MealWithDetails
import com.hakunakuinama.app.domain.model.GroceryItem
import com.hakunakuinama.app.domain.model.Ingredient
import com.hakunakuinama.app.domain.model.Meal
import com.hakunakuinama.app.domain.model.MealIngredient
import com.hakunakuinama.app.domain.model.MealStep

/**
 * Room graph → domain model.
 *
 * All sorting that the UI depends on happens here (not in SQL) so the ordering is
 * unit-testable and identical on every query path.
 */
fun MealWithDetails.toDomain(): Meal = Meal(
    id = meal.id,
    name = meal.name,
    tagline = meal.tagline,
    emoji = meal.emoji,
    imageUrl = meal.imageUrl,
    slot = meal.slot,
    difficulty = meal.difficulty,
    prepMinutes = meal.prepMinutes,
    cookMinutes = meal.cookMinutes,
    servings = meal.servings,
    tags = meal.tags,
    steps = steps.sortedBy { it.stepNumber }.map { it.toDomain() },
    ingredients = ingredients.map { it.toDomain() },
    isFavourite = meal.isFavourite,
)

fun MealStepEntity.toDomain(): MealStep = MealStep(
    number = stepNumber,
    instruction = instruction,
    durationMinutes = durationMinutes,
)

fun IngredientEntity.toDomain(): Ingredient = Ingredient(
    id = id,
    name = name,
    category = category,
    unit = unit,
    pricePerUnitKes = pricePerUnitKes,
    emoji = emoji,
    isStaple = isStaple,
)

fun GroceryItemEntity.toDomain(): GroceryItem = GroceryItem(
    id = id,
    name = name,
    emoji = emoji,
    category = category,
    quantity = quantity,
    unit = unit,
    unitPriceKes = unitPriceKes,
    isChecked = isChecked,
    sourceMealIds = sourceMealIds,
    weekStartEpochDay = weekStartEpochDay,
)

/** Resolves the join row + ingredient into a per-recipe usage. */
private fun com.hakunakuinama.app.data.local.entity.MealIngredientCrossRef.WithIngredient.toDomain(): MealIngredient =
    MealIngredient(
        ingredient = ingredient.toDomain(),
        quantity = usage.quantity,
        unit = usage.unit,
        isOptional = usage.isOptional,
        mustBuy = usage.mustBuy,
    )

package com.hakunakuinama.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * Many-to-many join between a recipe and an ingredient, carrying the per-recipe usage.
 *
 * Usage data lives here (not on [IngredientEntity]) because 0.5 kg of maize flour for
 * Ugali and 0.25 kg for githeri are different facts.
 *
 * @param mustBuy False for pantry staples assumed to be on hand (salt, cooking oil, water)
 *        — these never reach the shopping list.
 */
@Entity(
    tableName = "meal_ingredients",
    primaryKeys = ["mealId", "ingredientId"],
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = IngredientEntity::class,
            parentColumns = ["id"],
            childColumns = ["ingredientId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("mealId"), Index("ingredientId")],
)
data class MealIngredientCrossRef(
    val mealId: Long,
    val ingredientId: Long,
    val quantity: Double,
    val unit: String,
    val isOptional: Boolean = false,
    val mustBuy: Boolean = true,
) {
    /** Join row plus the ingredient it points at, for one meal. */
    data class WithIngredient(
        @Embedded val usage: MealIngredientCrossRef,
        @Relation(parentColumn = "ingredientId", entityColumn = "id")
        val ingredient: IngredientEntity,
    )
}

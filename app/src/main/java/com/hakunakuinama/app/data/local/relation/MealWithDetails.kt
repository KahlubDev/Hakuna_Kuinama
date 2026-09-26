package com.hakunakuinama.app.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.hakunakuinama.app.data.local.entity.MealEntity
import com.hakunakuinama.app.data.local.entity.MealIngredientCrossRef
import com.hakunakuinama.app.data.local.entity.MealStepEntity

/**
 * A recipe with everything the UI needs, in one query.
 *
 * The ingredient relation goes through the join row (rather than a flat `projection`) so
 * each usage keeps its own quantity/unit/optional flags — a projection-based relation
 * cannot distinguish "0.5 kg ugali flour" from "0.25 kg" for the same ingredient.
 */
data class MealWithDetails(
    @Embedded
    val meal: MealEntity,

    @Relation(parentColumn = "id", entityColumn = "mealId")
    val steps: List<MealStepEntity>,

    @Relation(
        entity = MealIngredientCrossRef::class,
        parentColumn = "id",
        entityColumn = "mealId",
    )
    val ingredients: List<MealIngredientCrossRef.WithIngredient>,
)

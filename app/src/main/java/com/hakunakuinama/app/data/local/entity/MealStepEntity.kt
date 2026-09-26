package com.hakunakuinama.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One instruction of a recipe, kept in its own table so ordering is explicit
 * (a step number stored on a CSV/text column cannot be re-sorted reliably).
 */
@Entity(
    tableName = "meal_steps",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("mealId")],
)
data class MealStepEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val mealId: Long,
    val stepNumber: Int,
    val instruction: String,
    val durationMinutes: Int? = null,
)

package com.hakunakuinama.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hakunakuinama.app.domain.model.FoodCategory

/**
 * The shared ingredient catalogue (not per-meal). Drives the Menu Builder chip grid.
 *
 * [isStaple] marks cheap Kenyan bachelor staples that should sort first in the picker.
 * [unit] is the *purchase* unit and [pricePerUnitKes] the KES price of one such unit.
 */
@Entity(
    tableName = "ingredients",
    indices = [Index(value = ["name"], unique = true), Index("category")],
)
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val category: FoodCategory,
    val unit: String,
    val pricePerUnitKes: Double,
    val emoji: String,
    @ColumnInfo(defaultValue = "0")
    val isStaple: Boolean = false,
    /** Optional per-serving defaults used when the user adds a manual shopping line. */
    @ColumnInfo(defaultValue = "1.0")
    val defaultQuantity: Double = 1.0,
)

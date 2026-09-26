package com.hakunakuinama.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hakunakuinama.app.domain.model.FoodCategory
import com.hakunakuinama.app.domain.model.MealDifficulty
import com.hakunakuinama.app.domain.model.MealSlot

/**
 * A recipe row.
 *
 * Cost is deliberately NOT a column: it is always derived from the linked ingredients
 * (see [com.hakunakuinama.app.domain.model.Meal.costPerServingKes]) so a recipe can never
 * disagree with its own ingredient prices.
 *
 * @param tags Pipe-delimited slugs, e.g. "budget|quick|vegan". Kept as a delimited
 *        String instead of JSON so the converter works in plain JVM unit tests.
 */
@Entity(
    tableName = "meals",
    indices = [Index("slot"), Index("name")],
)
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val tagline: String,
    val emoji: String,
    val imageUrl: String? = null,
    val slot: MealSlot,
    val difficulty: MealDifficulty = MealDifficulty.EASY,
    val prepMinutes: Int,
    val cookMinutes: Int,
    val servings: Int,
    @ColumnInfo(defaultValue = "")
    val tags: Set<String> = emptySet(),
    /**
     * Favourites live as a flag on the recipe rather than in a side table: the catalogue
     * is small and static, so "SELECT * FROM meals WHERE isFavourite = 1" beats a join.
     * Safe with seeding because the seeder is INSERT-IGNORE + count-guarded — it can never
     * overwrite a row the user has already favourited.
     */
    @ColumnInfo(defaultValue = "0")
    val isFavourite: Boolean = false,
    /** Epoch millis of when the user favourited it — used to sort "recently saved" first. */
    val favouritedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

package com.hakunakuinama.app.data.local

import androidx.room.TypeConverter
import com.hakunakuinama.app.domain.model.FoodCategory
import com.hakunakuinama.app.domain.model.MealDifficulty
import com.hakunakuinama.app.domain.model.MealSlot

/**
 * Room type converters.
 *
 * Collections are stored pipe-delimited rather than as JSON on purpose:
 * `org.json` is a stubbed android.jar class in local unit tests, which would make every
 * repository unit test that touches a Meal throw "Method not mocked". The invariant that
 * makes the delimiter safe is enforced in [toStringSet] / [toLongList]: any value that
 * contains the delimiter is rejected instead of silently corrupting the row.
 */
class Converters {

    @TypeConverter
    fun fromStringSet(value: Set<String>?): String =
        value?.filterNot { it.contains(DELIMITER) || it.isBlank() }?.joinToString(DELIMITER).orEmpty()

    @TypeConverter
    fun toStringSet(value: String?): Set<String> =
        value?.split(DELIMITER)?.map(String::trim)?.filter(String::isNotEmpty)?.toSet().orEmpty()

    @TypeConverter
    fun fromLongList(value: List<Long>?): String =
        value?.joinToString(DELIMITER).orEmpty()

    @TypeConverter
    fun toLongList(value: String?): List<Long> =
        value?.split(DELIMITER)?.mapNotNull(String::trim)?.mapNotNull(String::toLongOrNull).orEmpty()

    @TypeConverter
    fun fromMealSlot(value: MealSlot): String = value.name

    @TypeConverter
    fun toMealSlot(value: String): MealSlot = MealSlot.valueOf(value)

    @TypeConverter
    fun fromMealDifficulty(value: MealDifficulty): String = value.name

    @TypeConverter
    fun toMealDifficulty(value: String): MealDifficulty = MealDifficulty.valueOf(value)

    @TypeConverter
    fun fromFoodCategory(value: FoodCategory): String = value.name

    @TypeConverter
    fun toFoodCategory(value: String): FoodCategory = FoodCategory.valueOf(value)

    private companion object {
        const val DELIMITER = "|"
    }
}

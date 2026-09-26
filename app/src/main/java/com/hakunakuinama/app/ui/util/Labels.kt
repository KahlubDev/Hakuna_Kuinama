package com.hakunakuinama.app.ui.util

import androidx.annotation.StringRes
import com.hakunakuinama.app.R
import com.hakunakuinama.app.domain.model.FoodCategory
import com.hakunakuinama.app.domain.model.MealDifficulty
import com.hakunakuinama.app.domain.model.MealSlot
import java.text.NumberFormat
import java.util.Locale

/**
 * Presentation-layer mapping from domain enums to string resources.
 *
 * This indirection is the point: user-facing copy lives in `strings.xml`, not in the
 * domain model. A Shona/English bilingual build changes this file and nothing in
 * `domain/` — which is what keeps the domain free of Android and UI concerns.
 *
 * The same pattern applies to money: the domain hands over a `Double` in KES, and the
 * locale-aware formatting happens here.
 */

@StringRes
fun MealSlot.labelRes(): Int = when (this) {
    MealSlot.BREAKFAST -> R.string.slot_breakfast
    MealSlot.LUNCH -> R.string.slot_lunch
    MealSlot.DINNER -> R.string.slot_dinner
    MealSlot.SNACK -> R.string.slot_snack
}

@StringRes
fun FoodCategory.labelRes(): Int = when (this) {
    FoodCategory.PRODUCE -> R.string.category_produce
    FoodCategory.PROTEIN -> R.string.category_protein
    FoodCategory.STAPLE -> R.string.category_staple
    FoodCategory.DAIRY -> R.string.category_dairy
    FoodCategory.SPICES -> R.string.category_spices
    FoodCategory.OILS -> R.string.category_oils
    FoodCategory.OTHER -> R.string.category_other
}

@StringRes
fun MealDifficulty.labelRes(): Int = when (this) {
    MealDifficulty.EASY -> R.string.difficulty_easy
    MealDifficulty.MEDIUM -> R.string.difficulty_medium
    MealDifficulty.HARD -> R.string.difficulty_hard
}

/**
 * KES as a whole-shilling string with locale grouping ("1,240").
 *
 * Rounded rather than shown to two decimals on purpose: these are derived sums of
 * "0.5 kg times 120", and a shopper's decision is about whole shillings. Showing
 * "KES 111.28" implies a precision the ingredient prices do not have.
 */
fun Double.toKesAmount(): String =
    NumberFormat.getIntegerInstance(Locale.getDefault()).format(kotlin.math.round(this).toLong())

/**
 * A recipe quantity for display: "1", "0.5", "0.25".
 *
 * Trimmed of trailing zeros rather than formatted to a fixed width, because the grocery
 * list reads "0.5 kg maize flour" and never "0.50 kg".
 */
fun Double.toQuantityString(): String = when {
    this % 1.0 == 0.0 -> NumberFormat.getIntegerInstance(Locale.getDefault()).format(toLong())
    else -> String.format(Locale.getDefault(), "%.2f", this).trimEnd('0').trimEnd('.')
}

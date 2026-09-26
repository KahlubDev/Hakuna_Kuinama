package com.hakunakuinama.app.data.repository

import com.hakunakuinama.app.domain.model.Meal
import com.hakunakuinama.app.domain.model.MealIngredient
import com.hakunakuinama.app.testing.testIngredient
import com.hakunakuinama.app.testing.testMeal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two pieces of business logic that live in the data layer, plus the deterministic
 * daily suggestion.
 *
 * Both are `internal`, so this test lives in the same module — which is exactly why they
 * are worth testing here rather than being hidden behind the repository interface.
 */
class MealRepositoryLogicTest {

    private fun ingredient(id: Long, name: String, price: Double) =
        MealIngredient(
            ingredient = testIngredient(id, name, pricePerUnitKes = price),
            quantity = 0.5,
            unit = "kg",
            isOptional = false,
            mustBuy = true,
        )

    private fun mealWith(id: Long, vararg ingredients: MealIngredient, servings: Int = 2) = Meal(
        id = id,
        name = "Meal $id",
        tagline = "",
        emoji = "🍲",
        slot = com.hakunakuinama.app.domain.model.MealSlot.LUNCH,
        difficulty = com.hakunakuinama.app.domain.model.MealDifficulty.EASY,
        prepMinutes = 5,
        cookMinutes = 10,
        servings = servings,
        ingredients = ingredients.toList(),
    )

    // ------------------------------------------------------------ grocery merging

    @Test
    fun `two recipes needing the same ingredient produce one line with the quantities added`() {
        // The reason merging exists: picking Ugali *and* Githeri should not put maize flour
        // on the list twice.
        val ugali = mealWith(1, ingredient(1, "Maize flour", 120.0))
        val githeri = mealWith(2, ingredient(1, "Maize flour", 120.0))

        val lines = mergeIntoLines(listOf(ugali, githeri), servingsMultiplier = 1)

        assertEquals(1, lines.size)
        val line = lines.values.single()
        assertEquals(1.0, line.quantity, 0.001)
        assertEquals(120.0, line.quantity * line.unitPriceKes, 0.001)
        assertEquals(setOf(1L, 2L), line.sourceMealIds)
    }

    @Test
    fun `the servings multiplier scales the quantity`() {
        val meal = mealWith(1, ingredient(1, "Maize flour", 120.0))
        val forFour = mergeIntoLines(listOf(meal), servingsMultiplier = 2)
        assertEquals(1.0, forFour.values.single().quantity, 0.001)
    }

    @Test
    fun `a multiplier below one still works, for a half portion`() {
        val meal = mealWith(1, ingredient(1, "Maize flour", 120.0))
        val half = mergeIntoLines(listOf(meal), servingsMultiplier = 1)
        assertEquals(0.5, half.values.single().quantity, 0.001)
    }

    @Test
    fun `optional and pantry items never reach the shopping list`() {
        val salt = MealIngredient(
            ingredient = testIngredient(1, "Salt", pricePerUnitKes = 60.0),
            quantity = 0.05,
            unit = "kg",
            isOptional = false,
            mustBuy = false, // pantry
        )
        val eggs = MealIngredient(
            ingredient = testIngredient(2, "Eggs", pricePerUnitKes = 18.0),
            quantity = 2.0,
            unit = "piece",
            isOptional = true,
            mustBuy = true,
        )
        val oil = ingredient(3, "Cooking oil", 550.0)

        val lines = mergeIntoLines(listOf(mealWith(1, oil, salt, eggs)), servingsMultiplier = 1)

        assertEquals("only the oil should be on the list", setOf("Cooking oil"), lines.values.map { it.name }.toSet())
    }

    @Test
    fun `merging preserves the catalogue price and category`() {
        val lines = mergeIntoLines(listOf(mealWith(1, ingredient(1, "Tomatoes", 130.0))), 1)
        val line = lines.values.single()
        assertEquals(130.0, line.unitPriceKes, 0.001)
        assertEquals(com.hakunakuinama.app.domain.model.FoodCategory.STAPLE, line.category)
    }

    @Test
    fun `an empty meal list produces an empty merge`() {
        assertTrue(mergeIntoLines(emptyList(), 1).isEmpty())
    }

    @Test
    fun `a meal with nothing to buy contributes no lines`() {
        val bare = testMeal(9, "Nothing to buy")
        assertTrue(mergeIntoLines(listOf(bare), 1).isEmpty())
    }

    // ------------------------------------------------------- deterministic suggestion

    @Test
    fun `an empty catalogue suggests nothing rather than crashing`() {
        assertNull(
            "an empty catalogue has nothing to suggest, and must not throw",
            suggestedMealFor(emptyList(), com.hakunakuinama.app.domain.model.MealSlot.LUNCH, LocalDate.of(2026, 9, 26)),
        )
    }

    @Test
    fun `the same day and slot always pick the same meal`() {
        val meals = listOf(testMeal(1), testMeal(2), testMeal(3))
        val day = LocalDate.of(2026, 9, 26)

        val first = suggestedMealFor(meals, com.hakunakuinama.app.domain.model.MealSlot.LUNCH, day)
        val second = suggestedMealFor(meals, com.hakunakuinama.app.domain.model.MealSlot.LUNCH, day)
        assertEquals(first?.id, second?.id)
    }

    @Test
    fun `the suggestion rotates across a week`() {
        val meals = listOf(testMeal(1), testMeal(2), testMeal(3))
        val start = LocalDate.of(2026, 9, 26)
        val distinct = (0..6).map { offset ->
            suggestedMealFor(meals, com.hakunakuinama.app.domain.model.MealSlot.LUNCH, start.plusDays(offset.toLong()))?.id
        }.toSet()
        assertTrue("a week must not show the same meal every day", distinct.size > 1)
    }

    @Test
    fun `a far-future date still lands inside the list`() {
        val meals = listOf(testMeal(1), testMeal(2))
        val faraway = LocalDate.of(2077, 1, 1)
        repeat(50) { offset ->
            val meal = suggestedMealFor(meals, com.hakunakuinama.app.domain.model.MealSlot.DINNER, faraway.plusDays(offset.toLong()))
            assertTrue("index out of bounds on day $offset", meals.any { it.id == meal?.id })
        }
    }
}

package com.hakunakuinama.app.domain.model

import com.hakunakuinama.app.testing.testMeal
import com.hakunakuinama.app.testing.testMealIngredient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Menu Builder's scoring rules.
 *
 * This is the product's core claim — "tell me what I can cook" — so it is worth pinning
 * precisely, including the cases where a naive implementation is wrong.
 */
class MealMatchTest {

    private fun mealWith(vararg ingredientIds: Long, servings: Int = 2) =
        com.hakunakuinama.app.domain.model.Meal(
            id = 1,
            name = "Ugali & Sukuma",
            tagline = "",
            emoji = "🥬",
            slot = MealSlot.LUNCH,
            difficulty = MealDifficulty.EASY,
            prepMinutes = 10,
            cookMinutes = 25,
            servings = servings,
            ingredients = ingredientIds.map { testMealIngredient(it) },
        )

    @Test
    fun `everything ticked is a 100 percent cook-now match`() {
        val meal = mealWith(1, 2, 3, 4)
        val match = MealMatch.of(meal, setOf(1, 2, 3, 4))

        assertEquals(100, match.matchPercentage)
        assertTrue(match.canCookNow)
        assertTrue(match.missingIngredients.isEmpty())
        assertEquals(setOf(1L, 2L, 3L, 4L), match.matchedIngredientIds)
    }

    @Test
    fun `nothing ticked scores zero and lists everything as missing`() {
        val meal = mealWith(1, 2, 3, 4)
        val match = MealMatch.of(meal, emptySet())

        assertEquals(0, match.matchPercentage)
        assertFalse(match.canCookNow)
        assertEquals(4, match.missingIngredients.size)
    }

    @Test
    fun `the percentage is the share of required ingredients held`() {
        val match = MealMatch.of(mealWith(1, 2, 3, 4), setOf(1, 2))
        assertEquals(50, match.matchPercentage)
    }

    @Test
    fun `unknown ids do not count as a match`() {
        val match = MealMatch.of(mealWith(1, 2, 3, 4), setOf(999))
        assertEquals(0, match.matchPercentage)
    }

    @Test
    fun `missing ingredients are ordered cheapest first`() {
        // Cheapest (20) < medium (100) < dearest (500). The user is short of all three, and
        // the first thing they should be told is the cheapest way to close the gap.
        val meal = com.hakunakuinama.app.domain.model.Meal(
            id = 1,
            name = "Pilau",
            tagline = "",
            emoji = "🍗",
            slot = MealSlot.DINNER,
            difficulty = MealDifficulty.MEDIUM,
            prepMinutes = 20,
            cookMinutes = 45,
            servings = 2,
            ingredients = listOf(
                testMealIngredient(1, "Rice", pricePerUnitKes = 100.0),
                testMealIngredient(2, "Chicken", pricePerUnitKes = 500.0),
                testMealIngredient(3, "Salt", pricePerUnitKes = 20.0),
            ),
        )
        val match = MealMatch.of(meal, emptySet())

        assertEquals(listOf(3L, 1L, 2L), match.missingIngredients.map { it.ingredient.id })
        assertEquals(310.0, match.missingCostKes, 0.01)
    }

    @Test
    fun `optional ingredients never block a match and never count toward it`() {
        val base = mealWith(1, 2, 3, 4)
        val withOptional = base.copy(
            ingredients = base.ingredients + testMealIngredient(99, "Eggs", isOptional = true),
        )

        val withoutEggs = MealMatch.of(withOptional, setOf(1, 2, 3, 4))
        val withEggs = MealMatch.of(withOptional, setOf(1, 2, 3, 4, 99))

        assertEquals(100, withoutEggs.matchPercentage)
        assertEquals("an optional item must not change the score", withoutEggs.matchPercentage, withEggs.matchPercentage)
        assertTrue(withoutEggs.canCookNow)
    }

    @Test
    fun `a recipe with no required ingredients is trivially cookable`() {
        val match = MealMatch.of(mealWith(), emptySet())
        assertEquals(100, match.matchPercentage)
        assertTrue(match.canCookNow)
    }

    @Test
    fun `a recipe with no ingredients at all is treated as having no requirements`() {
        // Guard for the case above: an empty ingredient list must not divide by zero.
        val match = MealMatch.of(testMeal(9, "Mystery"), emptySet())
        assertEquals(100, match.matchPercentage)
    }

    @Test
    fun `a pantry staple is not something the user has to go and buy`() {
        // The regression this pins: `required` used to be `filterNot { it.isOptional }`,
        // which counted table salt. Every one of the four salted recipes then sat at
        // "9/10" with "Cook now" unreachable, for a user who had everything in front of
        // them. Salt is priced at zero and never reaches the shopping list, so scoring it
        // as missing contradicted the recipe screen as well.
        val base = mealWith(1, 2, 3)
        val withSalt = base.copy(
            ingredients = base.ingredients + testMealIngredient(98, "Table salt", mustBuy = false),
        )

        val match = MealMatch.of(withSalt, setOf(1, 2, 3))

        assertEquals(100, match.matchPercentage)
        assertTrue(match.canCookNow)
        assertTrue(match.missingIngredients.isEmpty())
        assertEquals("the staple must not appear as a match either", setOf(1L, 2L, 3L), match.matchedIngredientIds)
    }

    @Test
    fun `a pantry staple does not dilute the score of the things that must be bought`() {
        // Same defect from the other side: the staple must leave the denominator alone, so
        // "2 of 3" stays 2 of 3 rather than being reported as 2 of 4.
        val base = mealWith(1, 2, 3)
        val withSalt = base.copy(
            ingredients = base.ingredients + testMealIngredient(98, "Table salt", mustBuy = false),
        )

        val match = MealMatch.of(withSalt, setOf(1, 2))

        assertEquals(66, match.matchPercentage)
        assertFalse(match.canCookNow)
        assertEquals(listOf(3L), match.missingIngredients.map { it.ingredient.id })
    }
}

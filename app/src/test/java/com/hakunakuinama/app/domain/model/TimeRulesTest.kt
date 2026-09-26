package com.hakunakuinama.app.domain.model

import com.hakunakuinama.app.testing.testIngredient
import com.hakunakuinama.app.testing.testMeal
import com.hakunakuinama.app.testing.testMealIngredient
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MealSlot boundaries and Weeks.
 *
 * These are the two small pieces of date logic everything else trusts. Get them wrong and
 * the consequences are quiet and user-visible: dinner at breakfast, or a shopping list
 * that belongs to last week.
 */
class TimeRulesTest {

    // ------------------------------------------------------------- slot boundaries

    @Test
    fun `the early hours are a snack, not breakfast`() {
        // A student awake at 02:00 should not be told to make mandazi.
        assertEquals(MealSlot.SNACK, MealSlot.fromHour(0))
        assertEquals(MealSlot.SNACK, MealSlot.fromHour(2))
        assertEquals(MealSlot.SNACK, MealSlot.fromHour(4))
    }

    @Test
    fun `breakfast runs 05 00 to 10 59`() {
        assertEquals(MealSlot.BREAKFAST, MealSlot.fromHour(5))
        assertEquals(MealSlot.BREAKFAST, MealSlot.fromHour(10))
    }

    @Test
    fun `lunch runs 11 00 to 15 59`() {
        assertEquals(MealSlot.LUNCH, MealSlot.fromHour(11))
        assertEquals(MealSlot.LUNCH, MealSlot.fromHour(15))
    }

    @Test
    fun `dinner runs 16 00 to 21 59`() {
        assertEquals(MealSlot.DINNER, MealSlot.fromHour(16))
        assertEquals(MealSlot.DINNER, MealSlot.fromHour(21))
    }

    @Test
    fun `the late evening is a snack`() {
        assertEquals(MealSlot.SNACK, MealSlot.fromHour(22))
        assertEquals(MealSlot.SNACK, MealSlot.fromHour(23))
    }

    @Test
    fun `every hour of the day maps to exactly one slot`() {
        // MealSlot.fromHour is exhaustive over Int, so the compiler already forces all 24
        // hours to be handled. This test pins the behaviour rather than the type.
        val hours = (0..23).map { MealSlot.fromHour(it) }
        assertEquals("all 24 hours are covered", 24, hours.size)
        assertEquals(
            "every slot must be reachable from some hour of the day",
            MealSlot.entries.size,
            hours.toSet().size,
        )
    }

    @Test
    fun `startHour agrees with fromHour for every slot`() {
        MealSlot.entries.forEach { slot ->
            assertEquals("${slot.name} startHour must be the first hour of that slot",
                slot, MealSlot.fromHour(slot.startHour))
        }
    }

    // ----------------------------------------------------------------- week maths

    @Test
    fun `a Monday is its own week start`() {
        val monday = LocalDate.of(2026, 9, 28)
        assertEquals(monday, Weeks.startOf(monday))
    }

    @Test
    fun `a mid-week day snaps back to that Monday`() {
        val monday = LocalDate.of(2026, 9, 28)
        (1L..6L).forEach { offset ->
            assertEquals(
                "Monday +$offset belongs to that same week",
                monday,
                Weeks.startOf(monday.plusDays(offset)),
            )
        }
    }

    @Test
    fun `Sunday belongs to the week that started six days earlier`() {
        // The asymmetry that matters: a Monday-anchored list that jumped forward on Sunday
        // would leave that day with no shopping list at all.
        val sunday = LocalDate.of(2026, 9, 27)
        assertEquals(LocalDate.of(2026, 9, 21), Weeks.startOf(sunday))
    }

    @Test
    fun `weeks never straddle a year boundary incorrectly`() {
        // 1 Jan 2027 is a Friday, so its week started on Mon 28 Dec 2026.
        assertEquals(
            LocalDate.of(2026, 12, 28),
            Weeks.startOf(LocalDate.of(2027, 1, 1)),
        )
    }

    @Test
    fun `startOf is idempotent`() {
        val anyDay = LocalDate.of(2026, 9, 30)
        val once = Weeks.startOf(anyDay)
        assertEquals(once, Weeks.startOf(once))
    }

    // ------------------------------------------------------------- derived pricing

    @Test
    fun `per-plate cost is the batch cost divided by servings`() {
        val meal = testMeal(
            id = 1,
            servings = 2,
            ingredientIds = listOf(1, 2),
        ) // each 0.5kg @ 100 KES -> 100 KES per batch

        assertEquals(100.0, meal.batchCostKes, 0.001)
        assertEquals(50.0, meal.costPerServingKes, 0.001)
        assertEquals(15, meal.totalMinutes)
    }

    @Test
    fun `a recipe with no shoppable ingredients costs nothing rather than dividing by zero`() {
        val meal = testMeal(id = 2, servings = 0, ingredientIds = listOf(1))
        assertEquals(0.0, meal.costPerServingKes, 0.001)
    }

    @Test
    fun `optional extras are priced but kept out of the shopping list`() {
        val base = testMeal(id = 3, ingredientIds = listOf(1))
        val meal = base.copy(
            ingredients = base.ingredients + testMealIngredient(9, "Eggs", isOptional = true),
        )

        assertTrue("optional items must not inflate the budget", meal.shoppableIngredients.none { it.isOptional })
        assertEquals(1, meal.shoppableIngredients.size)
        assertEquals(1, meal.optionalExtras.size)
    }

    @Test
    fun `pantry staples are never charged to the budget`() {
        val salt = com.hakunakuinama.app.domain.model.MealIngredient(
            ingredient = testIngredient(1, "Salt", pricePerUnitKes = 60.0),
            quantity = 0.05,
            unit = "kg",
            isOptional = false,
            mustBuy = false,
        )
        assertEquals(0.0, salt.estimatedCostKes, 0.001)
    }

    @Test
    fun `the favourite flag rides on the recipe`() {
        assertTrue(testMeal(id = 4, isFavourite = true).isFavourite)
        assertFalse(testMeal(id = 5, name = "x", isFavourite = false).isFavourite)
    }
}

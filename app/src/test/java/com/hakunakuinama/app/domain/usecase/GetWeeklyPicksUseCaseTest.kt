package com.hakunakuinama.app.domain.usecase

import com.hakunakuinama.app.domain.model.FoodCategory
import com.hakunakuinama.app.domain.model.Ingredient
import com.hakunakuinama.app.domain.model.Meal
import com.hakunakuinama.app.domain.model.MealDifficulty
import com.hakunakuinama.app.domain.model.MealIngredient
import com.hakunakuinama.app.domain.model.MealSlot
import com.hakunakuinama.app.domain.repository.MealRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * "This week's picks" — the catalogue the Home screen lists under the hero.
 *
 * The ordering rule is the whole point of the use case, so it is asserted directly rather
 * than only through the ViewModel.
 */
class GetWeeklyPicksUseCaseTest {

    private val repository: MealRepository = mockk()

    /**
     * A one-ingredient recipe whose cost per plate is exactly [perPlate]: half a unit at
     * [perPlate] per kg, over two servings. The unit price and the plate price are the same
     * number, which is what makes the expected orderings readable.
     */
    private fun meal(id: Long, name: String, perPlate: Double) = Meal(
        id = id,
        name = name,
        tagline = "Tagline",
        emoji = "🍲",
        slot = MealSlot.LUNCH,
        difficulty = MealDifficulty.EASY,
        prepMinutes = 5,
        cookMinutes = 10,
        // Two servings of half a unit at `ingredientPrice`, so costPerServing == that price
        // and a test can say "KES 131" without doing arithmetic.
        servings = 2,
        ingredients = listOf(
            MealIngredient(
                ingredient = Ingredient(
                    id = id,
                    name = "Thing $id",
                    category = FoodCategory.STAPLE,
                    unit = "kg",
                    pricePerUnitKes = perPlate,
                    emoji = "🌽",
                    isStaple = true,
                ),
                quantity = 0.5,
                unit = "kg",
                isOptional = false,
                mustBuy = true,
            ),
        ),
    )

    @Test
    fun `cheapest plate first`() = runTest {
        val picks = listOf(
            meal(1, "Pilau ya haraka", 241.0),
            meal(2, "Githeri ya smoky", 131.0),
            meal(3, "Sukuma & ugali", 111.0),
        )
        every { repository.observeMeals() } returns MutableStateFlow(picks)

        val ordered = GetWeeklyPicksUseCase(repository)().first()

        assertEquals(
            listOf("Sukuma & ugali", "Githeri ya smoky", "Pilau ya haraka"),
            ordered.map { it.name },
        )
    }

    @Test
    fun `equal prices fall back to the name, so the list cannot reshuffle`() = runTest {
        val picks = listOf(
            meal(1, "Zucchini stew", 131.0),
            meal(2, "Avocado toast", 131.0),
        )
        every { repository.observeMeals() } returns MutableStateFlow(picks)

        val ordered = GetWeeklyPicksUseCase(repository)().first()

        assertEquals(listOf("Avocado toast", "Zucchini stew"), ordered.map { it.name })
    }

    @Test
    fun `an empty catalogue is an empty list, not an error`() = runTest {
        every { repository.observeMeals() } returns MutableStateFlow(emptyList())

        assertEquals(emptyList<Meal>(), GetWeeklyPicksUseCase(repository)().first())
    }
}

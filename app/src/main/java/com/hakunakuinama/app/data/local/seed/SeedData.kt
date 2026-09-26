package com.hakunakuinama.app.data.local.seed

import com.hakunakuinama.app.domain.model.FoodCategory
import com.hakunakuinama.app.domain.model.MealDifficulty
import com.hakunakuinama.app.domain.model.MealSlot

/**
 * The seed catalogue: pure Kotlin, zero Room/Android imports, so it can be asserted on in
 * a plain JVM unit test (see `SeedDataTest` in Phase 4).
 *
 * Ingredient prices are realistic Nairobi retail prices in KES for one [IngredientSeed.unit]
 * (e.g. maize flour is 120 KES per kilogram). A recipe's cost is never written down — it is
 * derived from these prices by `Meal.costPerServingKes`, so the two can never disagree.
 */
internal data class IngredientSeed(
    val name: String,
    val category: FoodCategory,
    val unit: String,
    val pricePerUnitKes: Double,
    val emoji: String,
    val isStaple: Boolean = false,
    val defaultQuantity: Double = 1.0,
)

/** One ingredient as used by one recipe. Referenced by name, resolved to an id at seed time. */
internal data class UsageSeed(
    val ingredient: String,
    val quantity: Double,
    val unit: String,
    val isOptional: Boolean = false,
    val mustBuy: Boolean = true,
)

internal data class StepSeed(
    val instruction: String,
    val durationMinutes: Int? = null,
)

internal data class MealSeed(
    val name: String,
    val tagline: String,
    val emoji: String,
    val slot: MealSlot,
    val difficulty: MealDifficulty = MealDifficulty.EASY,
    val prepMinutes: Int,
    val cookMinutes: Int,
    val servings: Int,
    val tags: Set<String>,
    val steps: List<StepSeed>,
    val ingredients: List<UsageSeed>,
)

internal object SeedData {

    val ingredients: List<IngredientSeed> = listOf(
        // ---- Staples & flour ----
        IngredientSeed("Maize flour (ugali)", FoodCategory.STAPLE, "kg", 120.0, "🌽", isStaple = true),
        IngredientSeed("Mandazi flour", FoodCategory.STAPLE, "kg", 130.0, "🍞", isStaple = true),
        IngredientSeed("Chapati flour", FoodCategory.STAPLE, "kg", 160.0, "🫓", isStaple = true),
        IngredientSeed("Rice", FoodCategory.STAPLE, "kg", 180.0, "🍚", isStaple = true),
        IngredientSeed("Table salt", FoodCategory.STAPLE, "kg", 60.0, "🧂", isStaple = true),
        IngredientSeed("Sugar", FoodCategory.STAPLE, "kg", 145.0, "🍬", isStaple = true),
        IngredientSeed("Bread", FoodCategory.STAPLE, "loaf", 80.0, "🥖"),

        // ---- Proteins ----
        IngredientSeed("Cowpeas (nyama beans)", FoodCategory.PROTEIN, "kg", 180.0, "🫘", isStaple = true),
        IngredientSeed("Green grams", FoodCategory.PROTEIN, "kg", 230.0, "🫛"),
        IngredientSeed("Chicken", FoodCategory.PROTEIN, "kg", 600.0, "🍗"),
        IngredientSeed("Eggs", FoodCategory.PROTEIN, "piece", 18.0, "🥚", defaultQuantity = 2.0),

        // ---- Produce ----
        IngredientSeed("Sukuma wiki", FoodCategory.PRODUCE, "bunch", 40.0, "🥬", isStaple = true),
        IngredientSeed("Tomatoes", FoodCategory.PRODUCE, "kg", 130.0, "🍅", isStaple = true),
        IngredientSeed("Onions", FoodCategory.PRODUCE, "kg", 150.0, "🧅", isStaple = true),
        IngredientSeed("Potatoes", FoodCategory.PRODUCE, "kg", 100.0, "🥔", isStaple = true),
        IngredientSeed("Carrots", FoodCategory.PRODUCE, "kg", 120.0, "🥕", isStaple = true),
        IngredientSeed("Green capsicum", FoodCategory.PRODUCE, "250 g piece", 80.0, "🫑"),
        IngredientSeed("Ginger", FoodCategory.PRODUCE, "100 g piece", 60.0, "🫚", isStaple = true),
        IngredientSeed("Garlic", FoodCategory.PRODUCE, "100 g piece", 70.0, "🧄", isStaple = true),
        IngredientSeed("Lemon", FoodCategory.PRODUCE, "piece", 20.0, "🍋"),
        IngredientSeed("Coriander", FoodCategory.PRODUCE, "bunch", 30.0, "🍃"),

        // ---- Dairy ----
        IngredientSeed("Milk", FoodCategory.DAIRY, "500 ml pack", 140.0, "🥛"),

        // ---- Spices ----
        IngredientSeed("Black tea leaves", FoodCategory.SPICES, "50 g pack", 90.0, "🍵", isStaple = true),
        IngredientSeed("Pili pili (chilli)", FoodCategory.SPICES, "50 g pack", 100.0, "🌶️"),
        IngredientSeed("Cardamom pods", FoodCategory.SPICES, "50 g pack", 200.0, "🌿"),
        IngredientSeed("Cinnamon sticks", FoodCategory.SPICES, "50 g pack", 250.0, "🪵"),

        // ---- Oils & fats ----
        IngredientSeed("Cooking oil", FoodCategory.OILS, "litre", 550.0, "🫗", isStaple = true),
    )

    val meals: List<MealSeed> = listOf(
        // ------------------------------------------------------------------ 1
        MealSeed(
            name = "Masala Chai & Mandazi",
            tagline = "Two cups of strong chai and hot mandazi before the 8am lecture",
            emoji = "🫖",
            slot = MealSlot.BREAKFAST,
            difficulty = MealDifficulty.EASY,
            prepMinutes = 5,
            cookMinutes = 15,
            servings = 2,
            tags = setOf("breakfast", "budget", "quick", "vegetarian", "tea"),
            steps = listOf(
                StepSeed("Bring 300 ml water to a boil in a small pot. The pot should be the size of a fist — a big pot cools too fast and the chai turns weak.", 3),
                StepSeed("Add 1 teaspoon of tea leaves and boil hard for 3 minutes. This is your mfumangu: the longer it boils, the stronger the tea.", 3),
                StepSeed("Strain the tea into a jug, then add 1 teaspoon sugar, 1 teaspoon finely grated ginger, 2 cardamom pods and a small piece of cinnamon.", 2),
                StepSeed("Pour in 300 ml milk and bring it back to a gentle simmer. Never boil milk once it is in — it curdles and tastes of tandoori.", 2),
                StepSeed("Meanwhile mix 250 g mandazi flour with 150 ml warm water and 1 tablespoon sugar into a stiff dough. Rest it while the chai simmers.", 5),
                StepSeed("Roll small balls, then flatten each with your thumb to the classic ring shape with a dent in the middle.", 2),
                StepSeed("Fry in hot oil until deep golden brown, about 3 minutes a side, and drain on a plate lined with a paper.", 6),
                StepSeed("Serve 2 cups of chai with the hot mandazi. Milk and sugar are the two levers if the week is tight."),
            ),
            ingredients = listOf(
                UsageSeed("Mandazi flour", 0.25, "kg"),
                UsageSeed("Black tea leaves", 0.10, "50 g pack"),
                UsageSeed("Milk", 0.60, "500 ml pack"),
                UsageSeed("Sugar", 0.03, "kg"),
                UsageSeed("Ginger", 0.02, "100 g piece"),
                UsageSeed("Cardamom pods", 0.02, "50 g pack"),
                UsageSeed("Cinnamon sticks", 0.02, "50 g pack"),
                UsageSeed("Cooking oil", 0.15, "litre"),
            ),
        ),
        // ------------------------------------------------------------------ 2
        MealSeed(
            name = "Ugali & Sukuma Wiki",
            tagline = "Maize flour and greens — the cheapest real meal in Kenya",
            emoji = "🥬",
            slot = MealSlot.LUNCH,
            difficulty = MealDifficulty.EASY,
            prepMinutes = 10,
            cookMinutes = 25,
            servings = 2,
            tags = setOf("lunch", "budget", "vegan", "traditional", "filling"),
            steps = listOf(
                StepSeed("Chop 1 onion, 2 tomatoes and 1 bunch of sukuma wiki into small pieces. Wash the sukuma twice — the second wash is what removes the bitterness.", 8),
                StepSeed("Peel 3 cloves of garlic and crush them with a pinch of salt into a rough paste.", 2),
                StepSeed("Heat 2 tablespoons of oil in a sufuria or heavy pan and fry the onion until soft and golden. Do not rush this, it is the base of the whole stew.", 5),
                StepSeed("Add the garlic, then the tomatoes, and cook until the mixture thickens and the oil separates at the edges of the pan.", 7),
                StepSeed("Add the sukuma wiki, salt to taste and a splash of water. Cover and let it wilt for 5 minutes — it should stay bright green, not grey.", 6),
                StepSeed("In a clean pot, bring 1.5 litres of water to a boil.", 5),
                StepSeed("Whisk in 500 g maize flour a little at a time while stirring, then keep stirring for 8 to 10 minutes until the dough pulls away from the pot and looks like putty.", 10),
                StepSeed("Shape the dough into a flat plate, rest it 2 minutes so it sets, then cut into wedges and serve with the sukuma on the side."),
            ),
            ingredients = listOf(
                UsageSeed("Maize flour (ugali)", 0.50, "kg"),
                UsageSeed("Sukuma wiki", 2.0, "bunch"),
                UsageSeed("Tomatoes", 0.30, "kg"),
                UsageSeed("Onions", 0.15, "kg"),
                UsageSeed("Garlic", 0.03, "100 g piece"),
                UsageSeed("Table salt", 0.015, "kg", mustBuy = false),
                UsageSeed("Cooking oil", 0.04, "litre"),
                UsageSeed("Eggs", 2.0, "piece", isOptional = true),
            ),
        ),
        // ------------------------------------------------------------------ 3
        MealSeed(
            name = "Chapati & Nyama Beans",
            tagline = "Protein-heavy and cheap — the student lunch that actually fills you",
            emoji = "🫓",
            slot = MealSlot.LUNCH,
            difficulty = MealDifficulty.EASY,
            prepMinutes = 20,
            cookMinutes = 30,
            servings = 2,
            tags = setOf("lunch", "budget", "protein", "student", "quick"),
            steps = listOf(
                StepSeed("Soak 300 g cowpeas in water for 1 hour before you start, or grab a tin if you are short on time.", 60),
                StepSeed("Boil the cowpeas with 1 teaspoon salt until they are soft, about 25 minutes, then drain and keep the cooking water.", 25),
                StepSeed("Fry 1 chopped onion in 2 tablespoons of oil, then add chopped tomatoes, 1 chopped capsicum, the garlic and a pinch of pili pili. Simmer into a thick stew.", 10),
                StepSeed("Mix 500 g chapati flour with 1 teaspoon salt, 2 tablespoons oil and just enough warm water to make a soft dough, then rest it.", 10),
                StepSeed("Roll the dough into 4 balls, then flatten each to a thin round, dusting the counter with dry flour so they do not stick.", 5),
                StepSeed("Fry on a dry or lightly oiled pan over medium heat. Bubbles and brown spots mean it is ready to flip.", 8),
                StepSeed("Tip the drained beans into the stew with a splash of the bean water and simmer 5 minutes. Serve the stew over the chapati with raw onion and pili pili on the side.", 5),
            ),
            ingredients = listOf(
                UsageSeed("Chapati flour", 0.50, "kg"),
                UsageSeed("Cowpeas (nyama beans)", 0.30, "kg"),
                UsageSeed("Cooking oil", 0.06, "litre"),
                UsageSeed("Tomatoes", 0.30, "kg"),
                UsageSeed("Onions", 0.15, "kg"),
                UsageSeed("Green capsicum", 0.20, "250 g piece"),
                UsageSeed("Garlic", 0.02, "100 g piece"),
                UsageSeed("Pili pili (chilli)", 0.01, "50 g pack"),
                UsageSeed("Table salt", 0.01, "kg", mustBuy = false),
            ),
        ),
        // ------------------------------------------------------------------ 4
        MealSeed(
            name = "Githeri",
            tagline = "One pot, four vegetables, survives three days in the fridge",
            emoji = "🥘",
            slot = MealSlot.LUNCH,
            difficulty = MealDifficulty.EASY,
            prepMinutes = 10,
            cookMinutes = 35,
            servings = 3,
            tags = setOf("lunch", "budget", "vegan", "one-pot", "meal-prep", "filling"),
            steps = listOf(
                StepSeed("Wash 300 g green grams, then boil them in 1.5 litres of water with 1 teaspoon salt until they start to split, about 20 minutes. Keep the water — that is your stock.", 20),
                StepSeed("Chop 1 onion, 2 tomatoes, 3 carrots and 4 potatoes into small cubes.", 10),
                StepSeed("Heat 3 tablespoons of oil in the same pot and fry the onion until golden, then add the crushed garlic.", 5),
                StepSeed("Add the tomatoes and fry until the oil floats on top of the mixture.", 5),
                StepSeed("Add the chopped vegetables, stir, then pour in enough of the green gram water to just cover everything.", 2),
                StepSeed("Simmer covered, stirring now and then so the bottom does not catch, for 20 minutes until the vegetables are soft.", 20),
                StepSeed("While it simmers, cook 500 g maize flour as you would ugali.", 10),
                StepSeed("Season with salt, add a little pili pili if you like heat, and serve the thick stew with ugali. It is even better the next day."),
            ),
            ingredients = listOf(
                UsageSeed("Maize flour (ugali)", 0.50, "kg"),
                UsageSeed("Green grams", 0.30, "kg"),
                UsageSeed("Potatoes", 0.60, "kg"),
                UsageSeed("Carrots", 0.35, "kg"),
                UsageSeed("Tomatoes", 0.30, "kg"),
                UsageSeed("Onions", 0.20, "kg"),
                UsageSeed("Garlic", 0.02, "100 g piece"),
                UsageSeed("Cooking oil", 0.06, "litre"),
                UsageSeed("Table salt", 0.015, "kg", mustBuy = false),
                UsageSeed("Pili pili (chilli)", 0.01, "50 g pack", isOptional = true),
            ),
        ),
        // ------------------------------------------------------------------ 5
        MealSeed(
            name = "Chicken Pilau",
            tagline = "The weekend show-off: spiced rice that tastes like a celebration",
            emoji = "🍗",
            slot = MealSlot.DINNER,
            difficulty = MealDifficulty.MEDIUM,
            prepMinutes = 20,
            cookMinutes = 45,
            servings = 3,
            tags = setOf("dinner", "protein", "aromatic", "weekend", "show-off"),
            steps = listOf(
                StepSeed("Joint 600 g chicken into pieces, rinse, then pat completely dry with kitchen paper. Wet chicken will never brown — it will only stew.", 10),
                StepSeed("Fry the chicken in 3 tablespoons of oil over medium-high heat until golden on all sides, about 10 minutes. Do not move it too early or the skin tears. Remove and set aside.", 10),
                StepSeed("In the same pot, soften 1 chopped onion, 3 cloves of crushed garlic and 1 tablespoon of grated ginger.", 5),
                StepSeed("Add chopped tomatoes and cook until the mixture darkens and the oil separates, about 8 minutes.", 8),
                StepSeed("Stir in the washed rice, 2 teaspoons salt and 1 teaspoon pili pili, then return the chicken with 750 ml water.", 3),
                StepSeed("Cover, drop to the lowest heat and cook undisturbed for 25 minutes. Do not lift the lid — the rice needs the steam that is trapped under it.", 25),
                StepSeed("Fluff with a fork, squeeze half a lemon over the top and scatter coriander.", 2),
                StepSeed("If you have a loaf of bread left, fry cubes of it in the leftover oil and toss them through. That single step is the difference between pilau and biryani.", 5),
            ),
            ingredients = listOf(
                UsageSeed("Rice", 0.75, "kg"),
                UsageSeed("Chicken", 0.60, "kg"),
                UsageSeed("Tomatoes", 0.40, "kg"),
                UsageSeed("Onions", 0.25, "kg"),
                UsageSeed("Cooking oil", 0.08, "litre"),
                UsageSeed("Garlic", 0.03, "100 g piece"),
                UsageSeed("Ginger", 0.02, "100 g piece"),
                UsageSeed("Pili pili (chilli)", 0.02, "50 g pack"),
                UsageSeed("Table salt", 0.02, "kg", mustBuy = false),
                UsageSeed("Lemon", 1.0, "piece"),
                UsageSeed("Coriander", 1.0, "bunch", isOptional = true),
                UsageSeed("Bread", 0.50, "loaf", isOptional = true),
            ),
        ),
    )
}

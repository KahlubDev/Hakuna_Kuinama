package com.hakunakuinama.app.ui.navigation

/**
 * Navigation argument keys.
 *
 * `ARG_RECIPE_ID` is the single source of truth for the recipe-detail argument: the route
 * declares it with `navArgument(ARG_RECIPE_ID)` and `RecipeDetailViewModel` reads it from
 * `SavedStateHandle` with the same constant. Two separate string literals would compile
 * fine and then fail at runtime with a "recipe not found" empty state, so there is only
 * one of them.
 *
 * The `LongType` in [recipeIdArgument] matters as much as the key: the ViewModel reads
 * the value as a `Long`, so a route typed as `IntType` would throw on retrieval.
 */
const val ARG_RECIPE_ID = "recipeId"

/**
 * Every destination in the app.
 *
 * Modelled as a sealed class rather than a bag of string constants so the compiler
 * refuses a typo'd route and so [bottomNavigationRoutes] can be the single source of
 * truth for the tab bar.
 */
sealed class Route(val route: String) {

    /** Dashboard: today's suggested meal. */
    object Home : Route("home")

    /** Menu Builder: pick ingredients, get ranked recipes. */
    object MenuBuilder : Route("menu_builder")

    /** Saved recipes. */
    object Favorites : Route("favorites")

    /**
     * A single recipe, pushed on top of the tab bar.
     *
     * The pattern carries the argument placeholder; [build] is the only supported way to
     * navigate here, so the placeholder and the real value can never be spelled
     * differently in two files.
     */
    object RecipeDetail : Route("recipe/{$ARG_RECIPE_ID}") {
        fun build(recipeId: Long): String = "recipe/$recipeId"
    }
}

/** The three destinations shown in the bottom bar. Everything else is pushed over it. */
val bottomNavigationRoutes: List<Route> = listOf(Route.Home, Route.MenuBuilder, Route.Favorites)

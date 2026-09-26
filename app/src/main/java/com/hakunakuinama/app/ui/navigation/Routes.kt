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
 * The route objects themselves land in Group B.
 */
const val ARG_RECIPE_ID = "recipeId"

package com.hakunakuinama.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hakunakuinama.app.R
import com.hakunakuinama.app.domain.usecase.MealSuggestion
import com.hakunakuinama.app.ui.component.EmptyState
import com.hakunakuinama.app.ui.component.MealCard
import com.hakunakuinama.app.ui.util.labelRes
import com.hakunakuinama.app.ui.viewmodel.DashboardUiState

/**
 * The dashboard: today's suggestion, quick links, saved recipes.
 *
 * Stateless on purpose — it takes a [DashboardUiState] and emits callbacks, so it can be
 * previewed and screenshot-tested without Hilt, a ViewModel or a database. The route
 * composable next to it does the wiring.
 */
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onOpenRecipe: (Long) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onOpenBuilder: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    when (val state = uiState) {
        DashboardUiState.Loading -> EmptyState(
            title = stringResource(R.string.state_loading),
            body = stringResource(R.string.state_loading_body),
            isLoading = true,
            modifier = modifier,
        )

        is DashboardUiState.Error -> EmptyState(
            title = stringResource(R.string.state_error_title),
            body = stringResource(R.string.state_error_body),
            icon = Icons.Filled.Warning,
            modifier = modifier,
        )

        is DashboardUiState.Ready -> ReadyContent(
            state = state,
            onOpenRecipe = onOpenRecipe,
            onToggleFavorite = onToggleFavorite,
            onOpenBuilder = onOpenBuilder,
            modifier = modifier,
            contentPadding = contentPadding,
        )
    }
}

/**
 * The dashboard: a greeting, today's suggestion, and the saved recipes.
 *
 * Stateless on purpose — it takes a [DashboardUiState] and emits callbacks, so it can be
 * previewed and screenshot-tested without Hilt, a ViewModel or a database. The route
 * composable next to it does the wiring.
 */
@Composable
private fun ReadyContent(
    state: DashboardUiState.Ready,
    onOpenRecipe: (Long) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onOpenBuilder: () -> Unit,
    modifier: Modifier,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "greeting") {
            Text(
                // From the state, not from a clock read here: the ViewModel resolved it
                // from the injected Clock and it rolls over with the slot, so the hello
                // line and the meal below it can never greet one meal and suggest another.
                text = stringResource(state.greeting.labelRes),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        item(key = "hero") {
            SuggestedMealHero(
                suggestion = state.suggestion,
                onOpenRecipe = onOpenRecipe,
                onToggleFavorite = onToggleFavorite,
                onOpenBuilder = onOpenBuilder,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        if (state.favorites.isNotEmpty()) {
            item(key = "saved-header") {
                Text(
                    text = stringResource(R.string.dashboard_saved_recipes),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            item(key = "saved-row") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items = state.favorites, key = { it.id }) { meal ->
                        // A width, not a wrap: the compact card's image is square, so
                        // without one the row would offer a full-width card and the
                        // "shelf" affordance disappears. A width also keeps these from
                        // growing to the width of an ultrawide screen, where a 1:1 crop
                        // would tower over the text.
                        MealCard(
                            meal = meal,
                            onClick = { onOpenRecipe(meal.id) },
                            onFavoriteClick = { onToggleFavorite(meal.id) },
                            modifier = Modifier.width(160.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * The hero card: the meal the ViewModel picked for this slot.
 *
 * A [MealCard] in its hero variant rather than a card of its own — the slot label comes from
 * [MealSuggestion.slot], the value the ViewModel resolved from the clock and bundled with
 * the meal, and the UI deliberately does **not** call `LocalTime.now()` or re-derive the
 * time of day: at the instant the clock crosses a slot boundary, a locally recomputed
 * header and the already-selected meal disagree, and the user is told to eat the wrong thing.
 */
@Composable
private fun SuggestedMealHero(
    suggestion: MealSuggestion,
    onOpenRecipe: (Long) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onOpenBuilder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val slotLabel = stringResource(suggestion.slot.labelRes())
    val meal = suggestion.meal

    if (meal == null) {
        // No recipe for this slot yet. An empty state, not an error: the catalogue is
        // still warming up on first launch.
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            EmptyState(
                title = stringResource(R.string.dashboard_no_suggestion_title),
                body = stringResource(R.string.dashboard_no_suggestion_body),
                icon = Icons.Filled.Info,
                // The copy says "check back in a moment", which on its own is a dead end:
                // the user is told to wait with nothing to do while they wait. The Builder
                // is the one screen that can make a suggestion appear, so it goes here
                // rather than leaving the state with no way forward.
                action = {
                    FilledTonalButton(onClick = onOpenBuilder) {
                        Text(text = stringResource(R.string.action_build_menu))
                    }
                },
                // Explicit minimum height: EmptyState fills its parent, and inside a
                // LazyColumn item the height is unbounded, so it needs a bound to lay out
                // against. A *minimum*, not a fixed height — at 200% font scale a taller
                // title and body must be able to grow instead of being clipped.
                modifier = Modifier.heightIn(min = 200.dp),
            )
        }
        return
    }

    MealCard(
        meal = meal,
        onClick = { onOpenRecipe(meal.id) },
        modifier = modifier,
        isHero = true,
        eyebrow = stringResource(R.string.dashboard_suggested_for, slotLabel),
        onFavoriteClick = { onToggleFavorite(meal.id) },
    )
}

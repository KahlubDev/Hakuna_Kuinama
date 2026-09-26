package com.hakunakuinama.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hakunakuinama.app.R
import com.hakunakuinama.app.domain.model.Meal
import com.hakunakuinama.app.domain.usecase.MealSuggestion
import com.hakunakuinama.app.ui.component.CostBadge
import com.hakunakuinama.app.ui.component.EmptyState
import com.hakunakuinama.app.ui.component.RecipeImage
import com.hakunakuinama.app.ui.util.labelRes
import com.hakunakuinama.app.ui.util.toKesAmount
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
    onOpenBuilder: () -> Unit,
    onOpenFavorites: () -> Unit,
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
            onOpenBuilder = onOpenBuilder,
            onOpenFavorites = onOpenFavorites,
            modifier = modifier,
            contentPadding = contentPadding,
        )
    }
}

@Composable
private fun ReadyContent(
    state: DashboardUiState.Ready,
    onOpenRecipe: (Long) -> Unit,
    onOpenBuilder: () -> Unit,
    onOpenFavorites: () -> Unit,
    modifier: Modifier,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "hero") {
            SuggestedMealHero(
                suggestion = state.suggestion,
                onOpenRecipe = onOpenRecipe,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item(key = "quick-links") {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = stringResource(R.string.dashboard_quick_links),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickLinkCard(
                        icon = Icons.Filled.RestaurantMenu,
                        title = stringResource(R.string.dashboard_open_builder_title),
                        body = stringResource(R.string.dashboard_open_builder_body),
                        onClick = onOpenBuilder,
                        modifier = Modifier.weight(1f),
                    )
                    QuickLinkCard(
                        icon = Icons.Filled.Favorite,
                        title = stringResource(R.string.dashboard_open_favorites_title),
                        body = stringResource(
                            R.string.dashboard_saved_recipes_count,
                            state.favorites.size,
                        ),
                        onClick = onOpenFavorites,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (state.favorites.isNotEmpty()) {
            item(key = "saved-header") {
                Text(
                    text = stringResource(R.string.dashboard_saved_recipes),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item(key = "saved-row") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items = state.favorites, key = { it.id }) { meal ->
                        SavedRecipeCard(meal = meal, onClick = { onOpenRecipe(meal.id) })
                    }
                }
            }
        }
    }
}

/**
 * The hero card.
 *
 * The slot label comes from [MealSuggestion.slot] — the value the ViewModel resolved from
 * the clock and bundled with the meal. The UI deliberately does **not** call
 * `LocalTime.now()` or re-derive the time of day: at the instant the clock crosses a slot
 * boundary, a locally recomputed header and the already-selected meal disagree, and the
 * user is told to eat the wrong thing.
 */
@Composable
private fun SuggestedMealHero(
    suggestion: MealSuggestion,
    onOpenRecipe: (Long) -> Unit,
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
                // Explicit minimum height: EmptyState fills its parent, and inside a
                // LazyColumn item the height is unbounded, so it needs a bound to lay out
                // against. A *minimum*, not a fixed height — at 200% font scale a taller
                // title and body must be able to grow instead of being clipped.
                modifier = Modifier.heightIn(min = 200.dp),
            )
        }
        return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(R.string.action_open_recipe, meal.name),
                role = Role.Button,
            ) { onOpenRecipe(meal.id) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            RecipeImage(
                imageUrl = meal.imageUrl,
                emoji = meal.emoji,
                contentDescription = meal.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_suggested_for, slotLabel),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
                Text(
                    text = meal.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = meal.tagline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CostBadge(costPerServingKes = meal.costPerServingKes)
                    Text(
                        text = stringResource(R.string.recipe_meta_time, meal.totalMinutes),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickLinkCard(
    icon: ImageVector,
    title: String,
    body: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(
            onClickLabel = title,
            role = Role.Button,
            onClick = onClick,
        ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null, // the title beside it is the label
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun SavedRecipeCard(
    meal: Meal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .width(168.dp)
            // A minimum, not a fixed height: this card holds an emoji, a two-line title
            // and a price. At 200% font scale a fixed 148dp would clip the price off the
            // bottom of the card entirely.
            .heightIn(min = 148.dp)
            .clickable(
                onClickLabel = stringResource(R.string.action_open_recipe, meal.name),
                role = Role.Button,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = meal.emoji, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = meal.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.format_kes, meal.costPerServingKes.toKesAmount()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

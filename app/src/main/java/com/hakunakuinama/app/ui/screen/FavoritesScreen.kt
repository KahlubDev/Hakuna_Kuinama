package com.hakunakuinama.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hakunakuinama.app.R
import com.hakunakuinama.app.ui.component.EmptyState
import com.hakunakuinama.app.ui.component.MealCard
import com.hakunakuinama.app.ui.viewmodel.FavoritesUiState

/**
 * Saved recipes, with swipe-to-remove.
 *
 * Removal is immediate, with no confirmation dialog: a dialog for "remove a recipe I
 * saved by accident" is more friction than the mistake costs, and the recovery is a
 * re-tap on the heart. `confirmValueChange` performs the delete at the moment the swipe
 * is accepted — returning true lets the row animate away, and the list then re-emits
 * without it, so no manual state reset is needed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    uiState: FavoritesUiState,
    onRemove: (Long) -> Unit,
    onOpenRecipe: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    when (val state = uiState) {
        FavoritesUiState.Loading -> EmptyState(
            title = stringResource(R.string.state_loading),
            body = stringResource(R.string.state_loading_body),
            isLoading = true,
            modifier = modifier,
        )

        is FavoritesUiState.Error -> EmptyState(
            title = stringResource(R.string.state_error_title_favorites),
            body = stringResource(R.string.state_error_body),
            icon = Icons.Filled.Warning,
            modifier = modifier,
        )

        is FavoritesUiState.Ready -> {
            if (state.isEmpty) {
                EmptyState(
                    title = stringResource(R.string.favorites_empty_title),
                    body = stringResource(R.string.favorites_empty_body),
                    icon = Icons.Filled.Favorite,
                    modifier = modifier,
                )
                return
            }

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = state.favorites, key = { it.id }) { meal ->
                    SwipeToDismissBox(
                        state = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                // Only one direction removes, so there is a single
                                // gesture to learn and no accidental deletions.
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    onRemove(meal.id)
                                    true
                                } else {
                                    false
                                }
                            },
                            positionalThreshold = { distance -> distance * 0.4f },
                        ),
                        enableDismissFromStartToEnd = false,
                        backgroundContent = { RemoveBackground() },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        // The heart is the accessible equivalent of the swipe. A gesture
                        // is invisible to a screen reader and unusable with a switch
                        // device, so removing a favourite must also be reachable by
                        // focus. It is the same action and the same ViewModel call — the
                        // heart is already filled, and its label says "Remove from
                        // favorites".
                        MealCard(
                            meal = meal,
                            onClick = { onOpenRecipe(meal.id) },
                            onFavoriteClick = { onRemove(meal.id) },
                        )
                    }
                }

                item(key = "list-footer") {
                    Text(
                        text = pluralStringResource(R.plurals.favorites_count, state.favorites.size, state.favorites.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 8.dp),
                    )
                }
            }
        }
    }
}

/** The surface revealed behind a card as it is swiped away. */
@Composable
private fun RemoveBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            // The row's own semantics already announce what it is; the swipe target is
            // the gesture, not a button.
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(end = 24.dp),
        )
    }
}

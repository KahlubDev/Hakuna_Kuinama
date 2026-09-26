package com.hakunakuinama.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.hakunakuinama.app.R
import com.hakunakuinama.app.domain.model.Ingredient
import com.hakunakuinama.app.ui.component.EmptyState
import com.hakunakuinama.app.ui.component.IngredientChip
import com.hakunakuinama.app.ui.component.MealCard
import com.hakunakuinama.app.ui.viewmodel.MenuBuilderUiState

/**
 * Menu Builder: tick what you have, get ranked recipes.
 *
 * Stateless like every other screen — the filter text is passed in and lifted by the
 * route so it survives rotation. The *selection* is not here at all: it lives in the
 * ViewModel, which is what stops a user losing their ticks by peeking at a recipe and
 * coming back.
 *
 * Empty selection deliberately shows a prompt rather than all five recipes. The screen's
 * whole value is answering "what can I cook with what I have"; opening it to a plain meal
 * list teaches nothing and looks like the feature is broken.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MenuBuilderScreen(
    uiState: MenuBuilderUiState,
    filterText: String,
    onFilterTextChange: (String) -> Unit,
    onToggleIngredient: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onOpenRecipe: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    if (!uiState.isLoading && uiState.errorCause != null) {
        EmptyState(
            title = stringResource(R.string.state_error_title_builder),
            body = stringResource(R.string.state_error_body),
            icon = Icons.Filled.Warning,
            modifier = modifier,
        )
        return
    }

    val visibleIngredients: List<Ingredient> = remember(uiState.ingredients, filterText) {
        if (filterText.isBlank()) {
            uiState.ingredients
        } else {
            uiState.ingredients.filter { it.name.contains(filterText.trim(), ignoreCase = true) }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "search") {
            OutlinedTextField(
                value = filterText,
                onValueChange = onFilterTextChange,
                singleLine = true,
                label = { Text(stringResource(R.string.builder_filter_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        // The field's label already names it; announcing "search" too is noise.
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    if (filterText.isNotEmpty()) {
                        TextButton(onClick = { onFilterTextChange("") }) {
                            Text(stringResource(R.string.action_close))
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        if (uiState.selectedIngredientIds.isNotEmpty()) {
            item(key = "selection-header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(
                            R.string.builder_selection_count,
                            uiState.selectedIngredientIds.size,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    TextButton(onClick = onClearSelection) {
                        Text(stringResource(R.string.builder_clear_all))
                    }
                }
            }
        }

        item(key = "chips") {
            when {
                uiState.isLoading -> EmptyState(
                    title = stringResource(R.string.state_loading),
                    body = stringResource(R.string.state_loading_body),
                    isLoading = true,
                    modifier = Modifier.heightIn(min = 180.dp),
                )

                visibleIngredients.isEmpty() -> EmptyState(
                    title = stringResource(R.string.builder_no_results_title),
                    body = stringResource(R.string.builder_no_results_body),
                    modifier = Modifier.heightIn(min = 160.dp),
                )

                else -> FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // FlowRow's content is a composable scope, so a plain loop over the
                    // chips is the supported way to place them — not a LazyVerticalGrid,
                    // which cannot be nested inside a LazyColumn item.
                    visibleIngredients.forEach { ingredient ->
                        IngredientChip(
                            ingredient = ingredient,
                            isSelected = ingredient.id in uiState.selectedIngredientIds,
                            onClick = { onToggleIngredient(ingredient.id) },
                        )
                    }
                }
            }
        }

        when {
            uiState.showSelectionPrompt -> item(key = "prompt") {
                EmptyState(
                    title = stringResource(R.string.builder_prompt_title),
                    body = stringResource(R.string.builder_prompt_body),
                    modifier = Modifier.heightIn(min = 200.dp),
                )
            }

            uiState.showNoResults -> item(key = "no-results") {
                EmptyState(
                    title = stringResource(R.string.builder_no_results_title),
                    body = stringResource(R.string.builder_no_results_body),
                    modifier = Modifier.heightIn(min = 200.dp),
                )
            }

            else -> {
                item(key = "matches-header") {
                    Text(
                        text = stringResource(R.string.builder_matches_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                items(items = uiState.matches, key = { it.meal.id }) { match ->
                    MealCard(
                        meal = match.meal,
                        match = match,
                        onClick = { onOpenRecipe(match.meal.id) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

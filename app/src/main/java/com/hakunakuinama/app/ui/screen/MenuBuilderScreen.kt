package com.hakunakuinama.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hakunakuinama.app.R
import com.hakunakuinama.app.domain.model.Ingredient
import com.hakunakuinama.app.domain.model.MealMatch
import com.hakunakuinama.app.ui.component.BorderedEmptyState
import com.hakunakuinama.app.ui.component.EmptyState
import com.hakunakuinama.app.ui.component.IngredientGrid
import com.hakunakuinama.app.ui.component.MealRowCard
import com.hakunakuinama.app.ui.component.ScreenGutter
import com.hakunakuinama.app.ui.component.ScreenHeader
import com.hakunakuinama.app.ui.component.ScreenTopPadding
import com.hakunakuinama.app.ui.component.SectionHeaderRow
import com.hakunakuinama.app.ui.viewmodel.MenuBuilderUiState

/**
 * Menu Builder: tick what you have, get ranked recipes.
 *
 * Stateless like every other screen — the filter text is passed in and lifted by the route
 * so it survives rotation. The *selection* is not here at all: it lives in the ViewModel,
 * which is what stops a user losing their ticks by peeking at a recipe and coming back.
 *
 * Empty selection deliberately shows a prompt rather than all five recipes. The screen's
 * whole value is answering "what can I cook with what I have?"; opening it to a plain meal
 * list teaches nothing and looks like the feature is broken.
 *
 * [filterText] and [onFilterTextChange] have no field in front of them any more — the
 * design drops the search box in favour of paged tiles, and 27 ingredients across three
 * pages are findable without typing. They stay in the signature because the route already
 * hoists them and a screen should not delete a parameter just because it stopped needing
 * it; the filter is still applied below, so anything that sets it still works.
 */
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
        item(key = "header") {
            ScreenHeader(
                eyebrow = stringResource(R.string.eyebrow_menu_builder),
                title = stringResource(R.string.builder_title),
                // The second line is the same size as the English one and in the eyebrow
                // brown: it is the same question asked twice, so it is set as one block
                // rather than as a caption under a heading.
                subtitle = stringResource(R.string.builder_subtitle),
                isSubtitleTitleSized = true,
                modifier = Modifier.padding(
                    start = ScreenGutter,
                    end = ScreenGutter,
                    top = ScreenTopPadding,
                    bottom = 10.dp,
                ),
            )
        }

        item(key = "picker-header") {
            PickerHeader(
                selectedCount = uiState.selectedIngredientIds.size,
                onClearSelection = onClearSelection,
                modifier = Modifier.padding(horizontal = ScreenGutter),
            )
        }

        item(key = "grid") {
            when {
                uiState.isLoading -> BorderedEmptyState(
                    title = stringResource(R.string.state_loading),
                    body = stringResource(R.string.state_loading_body),
                    isLoading = true,
                    minHeight = 180.dp,
                    modifier = Modifier.padding(horizontal = ScreenGutter),
                )

                visibleIngredients.isEmpty() -> BorderedEmptyState(
                    title = stringResource(R.string.builder_no_results_title),
                    body = stringResource(R.string.builder_no_results_body),
                    minHeight = 180.dp,
                    modifier = Modifier.padding(horizontal = ScreenGutter),
                )

                else -> IngredientGrid(
                    ingredients = visibleIngredients,
                    selectedIds = uiState.selectedIngredientIds,
                    onToggle = onToggleIngredient,
                    modifier = Modifier.padding(horizontal = ScreenGutter),
                )
            }
        }

        when {
            uiState.showSelectionPrompt -> item(key = "prompt") {
                BorderedEmptyState(
                    title = stringResource(R.string.builder_prompt_title),
                    body = stringResource(R.string.builder_prompt_body),
                    minHeight = 200.dp,
                    modifier = Modifier.padding(horizontal = ScreenGutter),
                )
            }

            uiState.showNoResults -> item(key = "no-results") {
                BorderedEmptyState(
                    title = stringResource(R.string.builder_no_results_title),
                    body = stringResource(R.string.builder_no_results_body),
                    minHeight = 200.dp,
                    modifier = Modifier.padding(horizontal = ScreenGutter),
                )
            }

            else -> {
                item(key = "matches-header") {
                    SectionHeaderRow(
                        title = stringResource(R.string.builder_matches_title),
                        modifier = Modifier.padding(
                            start = ScreenGutter,
                            end = ScreenGutter,
                            top = 14.dp,
                            bottom = 2.dp,
                        ),
                        trailing = {
                            Text(
                                text = stringResource(R.string.builder_cheapest_first),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier
                                    .alignByBaseline()
                                    .padding(start = 12.dp),
                            )
                        },
                    )
                }
                items(items = uiState.matches, key = { it.meal.id }) { match ->
                    MealRowCard(
                        meal = match.meal,
                        onClick = { onOpenRecipe(match.meal.id) },
                        footnote = match.matchFootnote(),
                        modifier = Modifier.padding(horizontal = ScreenGutter),
                    )
                }
            }
        }
    }
}

/**
 * "TAP WHAT YOU HAVE" with the running count on the right.
 *
 * The count is the clear-selection control, not a separate button beside it. The design
 * draws it as plain teal text, and it is the only affordance for clearing once anything is
 * ticked — so it looks like a label and behaves as a button, with [Role.Button] and a click
 * label that says what tapping it will do. Right aligned and baseline-aligned with the
 * eyebrow, so the two read as one line rather than as two floating fragments.
 */
@Composable
private fun PickerHeader(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clearLabel = pluralStringResource(
        R.plurals.builder_selection_clear,
        selectedCount,
        selectedCount,
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = stringResource(R.string.builder_tap_what_you_have).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .alignByBaseline(),
        )
        if (selectedCount > 0) {
            Text(
                text = pluralStringResource(
                    R.plurals.builder_selection_count,
                    selectedCount,
                    selectedCount,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                modifier = Modifier
                    .alignByBaseline()
                    .padding(start = 12.dp)
                    .clickable(
                        onClickLabel = clearLabel,
                        role = Role.Button,
                        onClick = onClearSelection,
                    )
                    // Vertical padding to reach a 48dp target; the line itself is 16dp.
                    .padding(vertical = 16.dp),
            )
        }
    }
}

/**
 * "Cook now" or "5/5 ingredients" — the one line that says why a recipe is ranked here.
 *
 * Counted from the domain rather than reusing [MealMatch.matchPercentage], because the Menu
 * Builder is a shopping decision: "5/5 ingredients" answers "do I have enough?" at a
 * glance, where "100% match" makes the user do the arithmetic.
 *
 * The denominator is [com.hakunakuinama.app.domain.model.Meal.shoppableIngredients], read
 * straight off the meal rather than recounted here. That is the same list [MealMatch.of]
 * divides by, so the two can never disagree — the earlier `count { !it.isOptional }` was a
 * second copy of the rule and had already drifted, counting pantry staples that the matcher
 * no longer scores.
 */
@Composable
private fun MealMatch.matchFootnote(): String {
    if (canCookNow) return stringResource(R.string.match_cook_now)
    val required = meal.shoppableIngredients.size
    return stringResource(R.string.match_ingredients, matchedIngredientIds.size, required)
}


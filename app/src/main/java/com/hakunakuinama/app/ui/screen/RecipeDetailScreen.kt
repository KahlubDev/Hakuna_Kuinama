package com.hakunakuinama.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hakunakuinama.app.R
import com.hakunakuinama.app.domain.model.Meal
import com.hakunakuinama.app.domain.model.MealIngredient
import com.hakunakuinama.app.ui.component.EmptyState
import com.hakunakuinama.app.ui.component.RecipeImage
import com.hakunakuinama.app.ui.component.StepItem
import com.hakunakuinama.app.ui.util.labelRes
import com.hakunakuinama.app.ui.util.toCountString
import com.hakunakuinama.app.ui.util.toKesAmount
import com.hakunakuinama.app.ui.util.toQuantityString
import com.hakunakuinama.app.ui.viewmodel.GroceryListUiState
import com.hakunakuinama.app.ui.viewmodel.RecipeDetailUiState

/**
 * A single recipe: cost, an ingredient checklist you can tick, the method, and the
 * shopping-list sheet.
 *
 * The per-plate cost is read from [Meal.costPerServingKes] — derived from the recipe's own
 * ingredient prices. Nothing here can display a stored cost, because none exists.
 */
@Composable
fun RecipeDetailScreen(
    uiState: RecipeDetailUiState,
    groceryState: GroceryListUiState,
    checkedIngredientNames: Set<String>,
    onIngredientChecked: (String, Boolean) -> Unit,
    onToggleFavorite: () -> Unit,
    onGenerateShoppingList: () -> Unit,
    onGroceryItemChecked: (Long, Boolean) -> Unit,
    onSheetRequested: () -> Unit,
    onSheetDismissed: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    // The design has no app bar: the back arrow and the heart float over the artwork.
    // Every state still needs a visible way out, not just the loaded one, so the
    // loading / not-found / error branches get the same affordance through EmptyState's
    // action slot rather than being left with only the system back gesture.
    val backAction: @Composable () -> Unit = {
        TextButton(onClick = onBack) {
            Text(text = stringResource(R.string.action_back))
        }
    }

    when (val state = uiState) {
        RecipeDetailUiState.Loading -> EmptyState(
            title = stringResource(R.string.state_loading),
            body = stringResource(R.string.state_loading_body),
            isLoading = true,
            modifier = modifier,
            action = backAction,
        )

        // A stale deep link or a popped back stack entry is normal navigation, not a
        // failure, so it gets an explanation rather than an error.
        RecipeDetailUiState.NotFound -> EmptyState(
            title = stringResource(R.string.recipe_not_found_title),
            body = stringResource(R.string.recipe_not_found_body),
            icon = Icons.Filled.Info,
            modifier = modifier,
            action = backAction,
        )

        is RecipeDetailUiState.Error -> EmptyState(
            title = stringResource(R.string.state_error_title_recipe),
            body = stringResource(R.string.state_error_body),
            icon = Icons.Filled.Warning,
            modifier = modifier,
            action = backAction,
        )

        is RecipeDetailUiState.Ready -> RecipeContent(
            state = state,
            groceryState = groceryState,
            checkedIngredientNames = checkedIngredientNames,
            onIngredientChecked = onIngredientChecked,
            onToggleFavorite = onToggleFavorite,
            onGenerateShoppingList = onGenerateShoppingList,
            onGroceryItemChecked = onGroceryItemChecked,
            onSheetRequested = onSheetRequested,
            onSheetDismissed = onSheetDismissed,
            onBack = onBack,
            modifier = modifier,
            contentPadding = contentPadding,
        )
    }
}

@Composable
private fun RecipeContent(
    state: RecipeDetailUiState.Ready,
    groceryState: GroceryListUiState,
    checkedIngredientNames: Set<String>,
    onIngredientChecked: (String, Boolean) -> Unit,
    onToggleFavorite: () -> Unit,
    onGenerateShoppingList: () -> Unit,
    onGroceryItemChecked: (Long, Boolean) -> Unit,
    onSheetRequested: () -> Unit,
    onSheetDismissed: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
    contentPadding: PaddingValues,
) {
    val meal = state.meal
    // Required and shoppable only. [Meal.shoppableIngredients] and not
    // `ingredients.filter { it.mustBuy }`, because that also matches the optional extras
    // — which are already listed separately below, so every "fried egg on the side"
    // would appear twice.
    val shoppable = meal.shoppableIngredients
    val hasList = groceryState.items.isNotEmpty()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "image") {
                Box {
                    RecipeImage(
                        imageUrl = meal.imageUrl,
                        contentDescription = meal.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 10f),
                    )

                    // Both controls float over the artwork rather than sitting in a bar,
                    // so each carries its own scrim: the app promises to work offline, and
                    // an icon that vanishes into a pale photo is a dead end for the one
                    // person who most needs the back arrow.
                    OverlayIconButton(
                        onClick = onBack,
                        contentDescription = stringResource(R.string.action_back),
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                    )
                    OverlayIconButton(
                        onClick = onToggleFavorite,
                        contentDescription = stringResource(
                            if (state.isFavorite) {
                                R.string.action_favorite_remove
                            } else {
                                R.string.action_favorite_add
                            },
                        ),
                        icon = if (state.isFavorite) {
                            Icons.Filled.Favorite
                        } else {
                            Icons.Filled.FavoriteBorder
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                    )
                }
            }

            item(key = "header") {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = meal.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = meal.tagline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            item(key = "info") {
                InfoCard(
                    rows = listOf(
                        stringResource(R.string.recipe_meta_time_label) to
                            stringResource(R.string.recipe_meta_time, meal.totalMinutes),
                        stringResource(R.string.recipe_meta_servings_label) to
                            meal.servings.toCountString(),
                        stringResource(R.string.recipe_meta_difficulty_label) to
                            stringResource(meal.difficulty.labelRes()),
                        stringResource(R.string.recipe_meta_cost_label) to
                            stringResource(
                                R.string.format_kes,
                                meal.costPerServingKes.toKesAmount(),
                            ),
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            item(key = "ingredients-header") {
                SectionHeader(
                    title = stringResource(R.string.recipe_ingredients),
                    subtitle = stringResource(
                        R.string.recipe_checklist_progress,
                        checkedIngredientNames.count { name ->
                            shoppable.any { it.ingredient.name == name }
                        },
                        shoppable.size,
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            items(items = shoppable, key = { it.ingredient.id }) { usage ->
                IngredientChecklistRow(
                    usage = usage,
                    isChecked = usage.ingredient.name in checkedIngredientNames,
                    onCheckedChange = { onIngredientChecked(usage.ingredient.name, it) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            if (meal.optionalExtras.isNotEmpty()) {
                item(key = "extras-header") {
                    SectionHeader(
                        title = stringResource(R.string.recipe_optional_extras),
                        subtitle = stringResource(
                            R.string.recipe_optional_extras_cost,
                            meal.optionalExtras.sumOf { it.estimatedCostKes }.toKesAmount(),
                        ),
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                items(items = meal.optionalExtras, key = { "extra-${it.ingredient.id}" }) { usage ->
                    Text(
                        text = usage.ingredient.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }

            item(key = "steps-header") {
                SectionHeader(
                    title = stringResource(R.string.recipe_steps),
                    subtitle = null,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            items(items = meal.steps, key = { it.number }) { step ->
                StepItem(
                    stepNumber = step.number,
                    instruction = step.instruction,
                    durationMinutes = step.durationMinutes,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            // Room for the extended FAB so it never covers the last step.
            item(key = "fab-spacer") { Box(modifier = Modifier.height(88.dp)) }
        }

        ExtendedFloatingActionButton(
            // One button, two jobs. `onGenerateForRecipe` *replaces* the week's list, so
            // regenerating over a list the user has already ticked off would throw away
            // their work. Once a list exists the button reopens it instead.
            onClick = if (hasList) onSheetRequested else onGenerateShoppingList,
            icon = {
                Icon(
                    imageVector = Icons.Filled.ShoppingCart,
                    contentDescription = null,
                )
            },
            text = {
                Text(
                    stringResource(
                        if (hasList) R.string.recipe_view_list else R.string.recipe_generate_list,
                    ),
                )
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    }

    if (groceryState.isSheetVisible) {
        GroceryListSheet(
            groceryState = groceryState,
            mealName = meal.name,
            onItemChecked = onGroceryItemChecked,
            onDismissed = onSheetDismissed,
        )
    }
}

/**
 * A round, translucent button for use on top of the hero artwork.
 *
 * The scrim is a flat wash rather than the gradient the design language forbids, and it is
 * what guarantees the icon keeps its contrast: white on 45% black clears 4.5:1 over both
 * the pale geometric artwork and a dark photograph, where a bare icon would not.
 */
@Composable
private fun OverlayIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        // Surface's onClick gives a 48dp target and a real click role for free, which a
        // clickable Box would not.
        modifier = modifier.size(40.dp),
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.45f),
        contentColor = Color.White,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * The four facts about a recipe, as label/value rows on one card.
 *
 * Rows rather than pills: a pill says "35 min" with nothing to say which question that
 * answers, and four pills wrap onto two ragged lines on a narrow phone. A row has a label
 * that survives being read out of context, and the whole block is one bordered card, so
 * it still reads as a unit.
 */
@Composable
private fun InfoCard(
    rows: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            rows.forEachIndexed { index, (label, value) ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun IngredientChecklistRow(
    usage: MealIngredient,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val quantityLabel = stringResource(
        R.string.format_quantity,
        usage.quantity.toQuantityString(),
        usage.unit,
    )
    val costLabel = stringResource(
        R.string.format_kes,
        usage.estimatedCostKes.toKesAmount(),
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            // The whole row toggles, not just the 48dp box: a student shopping with one
            // thumb should not have to hit a 24dp target.
            .clickable(
                role = Role.Checkbox,
                onClick = { onCheckedChange(!isChecked) },
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = isChecked,
            // Null because the row already carries the click and the merged semantics;
            // a second one would double-announce and double-fire.
            onCheckedChange = null,
        )
        Column(modifier = Modifier.padding(start = 4.dp)) {
            Text(
                text = usage.ingredient.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textDecoration = if (isChecked) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.recipe_ingredient_detail, quantityLabel, costLabel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The shopping list, as a bottom sheet over the recipe.
 *
 * The sheet's visibility comes from the ViewModel, not a local boolean, so a rotation
 * mid-shop does not throw away a list the user is halfway through.
 *
 * The @OptIn is required: ModalBottomSheet and rememberModalBottomSheetState are still
 * @ExperimentalMaterial3Api in Material3 1.3.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroceryListSheet(
    groceryState: GroceryListUiState,
    mealName: String,
    onItemChecked: (Long, Boolean) -> Unit,
    onDismissed: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissed,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = stringResource(R.string.grocery_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.grocery_source_for, mealName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (groceryState.items.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.grocery_sheet_empty_title),
                    body = stringResource(R.string.grocery_sheet_empty_body),
                    modifier = Modifier.heightIn(min = 180.dp),
                )
            } else {
                LazyColumn(
                    // Bounded both ways: the sheet must not eat a whole short screen, and
                    // at 200% font scale the rows get taller and the list must be able to
                    // grow rather than clip.
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(items = groceryState.items, key = { it.id }) { item ->
                        val quantityLabel = stringResource(
                            R.string.format_quantity,
                            item.quantity.toQuantityString(),
                            item.unit,
                        )
                        val costLabel = stringResource(
                            R.string.format_kes,
                            item.estimatedCostKes.toKesAmount(),
                        )
                        // One merged semantics node per line: the name, how much, what it
                        // costs, and whether it is already in the basket. A screen-reader
                        // user shopping in a supermarket needs all four, in one utterance.
                        val stateLabel = stringResource(
                            if (item.isChecked) R.string.grocery_checked else R.string.grocery_not_checked,
                            item.name,
                        )
                        // Resolved here, not inside Modifier.semantics: that lambda is a
                        // plain (non-composable) scope, so a stringResource() call inside
                        // it does not compile.
                        val lineLabel = stringResource(
                            R.string.grocery_item_description,
                            item.name,
                            quantityLabel,
                            costLabel,
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    role = Role.Checkbox,
                                    onClick = { onItemChecked(item.id, !item.isChecked) },
                                )
                                .semantics(mergeDescendants = true) {
                                    contentDescription = "$lineLabel. $stateLabel"
                                    this.stateDescription = stateLabel
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = item.isChecked,
                                onCheckedChange = null,
                            )
                            Column(modifier = Modifier.padding(start = 4.dp)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (item.isChecked) {
                                        TextDecoration.LineThrough
                                    } else {
                                        null
                                    },
                                )
                                Text(
                                    text = stringResource(
                                        R.string.grocery_line_detail,
                                        quantityLabel,
                                        costLabel,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.grocery_sheet_remaining,
                            stringResource(R.string.format_kes, groceryState.summary.outstandingKes.toKesAmount())),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    FilledTonalButton(onClick = onDismissed) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Text(
                            text = stringResource(R.string.action_close),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

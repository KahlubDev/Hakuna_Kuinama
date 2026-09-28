package com.hakunakuinama.app.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.hakunakuinama.app.ui.component.ScreenGutter
import com.hakunakuinama.app.ui.component.ScreenTopPadding
import com.hakunakuinama.app.ui.component.Footnote
import com.hakunakuinama.app.ui.component.RecipeImage
import com.hakunakuinama.app.ui.component.StepItem
import com.hakunakuinama.app.ui.util.labelRes
import com.hakunakuinama.app.ui.util.toKesAmount
import com.hakunakuinama.app.ui.util.toQuantityString
import com.hakunakuinama.app.ui.viewmodel.GroceryListUiState
import com.hakunakuinama.app.ui.viewmodel.RecipeDetailUiState

/**
 * The banner's proportions, measured off the mockup: 242 × 92, full-bleed, with the
 * controls in a paper bar *above* it rather than floating on top of it.
 */
private const val DETAIL_IMAGE_ASPECT = 2.63f

/**
 * The width of the ingredient table's quantity column, so every name in the table starts
 * at the same x. A number that floats left of a ragged left edge is the fastest way to make
 * a table look like a list of unrelated lines.
 */
private val QuantityColumnWidth = 92.dp

/**
 * A single recipe: the price, an ingredient table, the method, and the shopping-list sheet.
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
    // — which are already listed as the table's last row, so every "fried egg on the side"
    // would appear twice.
    val shoppable = meal.shoppableIngredients
    val hasList = groceryState.items.isNotEmpty()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item(key = "top-bar") {
                RecipeTopBar(
                    onBack = onBack,
                    onToggleFavorite = onToggleFavorite,
                    isFavorite = state.isFavorite,
                )
            }

            item(key = "image") {
                RecipeImage(
                    imageUrl = meal.imageUrl,
                    contentDescription = meal.name,
                    artworkSeed = meal.id,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(DETAIL_IMAGE_ASPECT),
                )
            }

            item(key = "header") {
                Column(modifier = Modifier.padding(start = ScreenGutter, end = ScreenGutter, top = 22.dp)) {
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

            item(key = "price") {
                PriceLine(
                    costPerServingKes = meal.costPerServingKes,
                    difficultyLabel = stringResource(meal.difficulty.labelRes()),
                    timeLabel = stringResource(R.string.recipe_meta_time, meal.totalMinutes),
                    modifier = Modifier.padding(start = ScreenGutter, end = ScreenGutter, top = 20.dp),
                )
            }

            item(key = "ingredients-header") {
                SectionHeading(
                    title = stringResource(R.string.recipe_ingredients),
                    modifier = Modifier.padding(start = ScreenGutter, end = ScreenGutter, top = 34.dp, bottom = 6.dp),
                )
            }

            // One list item for the whole table rather than one per row, so the hairlines
            // between rows cannot drift apart as the column re-measures, and so the
            // dividers are the only thing between rows — which is what the design draws.
            item(key = "ingredients") {
                IngredientTable(
                    required = shoppable,
                    optional = meal.optionalExtras,
                    checkedNames = checkedIngredientNames,
                    onCheckedChange = onIngredientChecked,
                    modifier = Modifier.padding(horizontal = ScreenGutter),
                )
            }

            item(key = "extras-note") {
                Footnote(
                    text = stringResource(R.string.recipe_optional_extras_note),
                    modifier = Modifier.padding(start = ScreenGutter, end = ScreenGutter, top = 12.dp),
                )
            }

            item(key = "steps-header") {
                SectionHeading(
                    title = stringResource(R.string.recipe_steps),
                    modifier = Modifier.padding(start = ScreenGutter, end = ScreenGutter, top = 34.dp, bottom = 14.dp),
                )
            }

            items(items = meal.steps, key = { it.number }) { step ->
                StepItem(
                    stepNumber = step.number,
                    instruction = step.instruction,
                    durationMinutes = step.durationMinutes,
                    modifier = Modifier.padding(
                        start = ScreenGutter,
                        end = ScreenGutter,
                        bottom = 18.dp,
                    ),
                )
            }

            // Room for the extended FAB so it never covers the last step.
            item(key = "fab-spacer") { Box(modifier = Modifier.height(96.dp)) }
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
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(ScreenGutter),
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
 * The paper bar above the artwork, holding the back arrow and the heart.
 *
 * The controls sit on the page, not on the picture. The mockups put a strip of paper above
 * the banner with two plain ink icons on it, and that is also the more robust choice: an
 * icon floating on artwork needs a scrim to stay legible over a pale photo, and a scrim is
 * a dark disc the eye goes to first. On paper the icon is simply dark-on-light and needs
 * nothing behind it.
 */
@Composable
private fun RecipeTopBar(
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 4.dp, top = 6.dp, end = 4.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // IconButton's own 48dp is the touch target; the 20dp icon is the drawn size.
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(
                    if (isFavorite) R.string.action_favorite_remove else R.string.action_favorite_add,
                ),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * "KES 131 │ Easy · 25 min".
 *
 * One line, not the old four-row info card. A price this large is the answer to "can I
 * afford this?", and burying it under label/value rows four deep made the user read past
 * it. Difficulty and time follow it as a single muted aside — they are context for the
 * price, not facts the user came to look up.
 *
 * The separator is a drawn rule rather than a "|" character, so it is one hairline tall
 * instead of a full line of the text's height, which is what the design draws.
 */
@Composable
private fun PriceLine(
    costPerServingKes: Double,
    difficultyLabel: String,
    timeLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.format_kes, costPerServingKes.toKesAmount()),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
        Box(
            modifier = Modifier
                .size(width = 1.dp, height = 26.dp)
                .background(MaterialTheme.colorScheme.outline),
        )
        Text(
            text = stringResource(R.string.recipe_meta_summary, difficultyLabel, timeLabel),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A bare section heading, no card and no subtitle. */
@Composable
private fun SectionHeading(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier,
    )
}

/**
 * The ingredient table: a fixed quantity column, the name beside it, and a hairline
 * between rows.
 *
 * The design draws this as a printed table with no checkboxes, so that is what it is. The
 * tickable version is gone from this screen on purpose — the design's answer to "what do I
 * need?" is a list to read, and a column of 24dp boxes in front of every line turns a
 * readable table into a form. The shopping list itself is still fully tickable, in the
 * sheet the FAB opens, which is where ticking actually does work.
 *
 * The optional extras are the table's last row rather than a separate block, dimmed and
 * suffixed, because a recipe's optional ingredients belong next to its required ones — the
 * reader is deciding, not filing.
 */
@Composable
private fun IngredientTable(
    required: List<MealIngredient>,
    optional: List<MealIngredient>,
    checkedNames: Set<String>,
    onCheckedChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = remember(required, optional) { required + optional }

    Column(modifier = modifier) {
        rows.forEachIndexed { index, usage ->
            if (index > 0) {
                // `surfaceContainerHighest`, not `outlineVariant`. The mockups draw the
                // table's rules a step lighter than the card outlines, and they have to
                // differ: a rule the same weight as the card's own border reads as a second
                // box inside the card instead of as a table.
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)
            }
            IngredientRow(
                usage = usage,
                isOptional = index >= required.size,
                isChecked = usage.ingredient.name in checkedNames,
                onCheckedChange = { onCheckedChange(usage.ingredient.name, it) },
            )
        }
    }
}

/** One table row: quantity on the left, name on the right, hairline above it. */
@Composable
private fun IngredientRow(
    usage: MealIngredient,
    isOptional: Boolean,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val quantityLabel = stringResource(
        R.string.format_quantity,
        usage.quantity.toQuantityString(),
        usage.unit,
    )
    val name = usage.ingredient.name

    Row(
        modifier = modifier
            .fillMaxWidth()
            // The whole row toggles, not a 24dp box: a student shopping with one thumb
            // should not have to hit a small target. The 14dp vertical padding is what
            // brings the row to a 50dp touch height, matching the design's row rhythm.
            .clickable(
                onClickLabel = stringResource(
                    if (isChecked) R.string.recipe_ingredient_have else R.string.recipe_ingredient_need,
                    name,
                ),
                role = Role.Checkbox,
                onClick = { onCheckedChange(!isChecked) },
            )
            .padding(vertical = 14.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$quantityLabel, $name"
                stateDescription = stateDescription
            },
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = quantityLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // A fixed width, not weight(1f): the name column has to start at the same x on
            // every row, and a weighted column would give "1" and "2 tbsp" the same width
            // and then still let a long quantity push the names out of line.
            modifier = Modifier.width(QuantityColumnWidth),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (isOptional) {
                stringResource(R.string.format_optional_suffix, name)
            } else {
                name
            },
            style = MaterialTheme.typography.bodyLarge,
            color = if (isOptional) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textDecoration = if (isChecked) TextDecoration.LineThrough else null,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
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
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(horizontal = ScreenGutter)) {
            Text(
                text = stringResource(R.string.grocery_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.grocery_source_for, mealName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
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
                        text = stringResource(
                            R.string.grocery_sheet_remaining,
                            stringResource(
                                R.string.format_kes,
                                groceryState.summary.outstandingKes.toKesAmount(),
                            ),
                        ),
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

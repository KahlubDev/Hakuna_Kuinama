package com.hakunakuinama.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hakunakuinama.app.R
import com.hakunakuinama.app.domain.model.FoodCategory
import com.hakunakuinama.app.domain.model.Ingredient
import com.hakunakuinama.app.ui.util.labelRes
import com.hakunakuinama.app.ui.util.toKesAmount

/**
 * The kitchen grid: every ingredient, grouped by aisle, two to a row.
 *
 * Grouped rather than flat because a flat grid of 27 tiles is a wall — the user scrolls it
 * looking for maize flour. Aisle headings let them jump to "Staples & Flour" and stop.
 * The grouping comes from [FoodCategory] itself, so the order on screen is the order the
 * domain enum declares and no screen has to sort anything.
 *
 * Rows are built by hand instead of with a `LazyVerticalGrid`, for the same reason the
 * chips were in a [androidx.compose.foundation.layout.FlowRow]: a grid cannot be nested
 * inside a `LazyColumn` item, and this sits in one to keep the screen a single scrolling
 * column. The catalogue is a fixed ~27 rows' worth of data seeded from Room, so laying
 * every tile out eagerly is cheaper than it looks.
 */
@Composable
fun IngredientGrid(
    ingredients: List<Ingredient>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FoodCategory.entries.forEach { category ->
            val inCategory = ingredients.filter { it.category == category }
            if (inCategory.isEmpty()) return@forEach

            Text(
                text = stringResource(category.labelRes()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
            )

            inCategory.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { ingredient ->
                        IngredientTile(
                            ingredient = ingredient,
                            isSelected = ingredient.id in selectedIds,
                            onClick = { onToggle(ingredient.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // An odd row leaves a hole. A Spacer keeps the last tile at half
                    // width instead of stretching it, which would make one ingredient look
                    // like the feature of the row.
                    if (row.size == 1) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * One tile in the kitchen grid.
 *
 * Carries over the accessibility work from the FilterChip this replaces: the description
 * names the ingredient, its price and whether it is ticked, and [stateDescription] carries
 * the tick separately because that is the field TalkBack re-announces on return. The check
 * icon itself is unlabelled — the parent already said "selected", and two nodes saying it
 * makes the grid slower to move through.
 */
@Composable
fun IngredientTile(
    ingredient: Ingredient,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stateLabel = stringResource(
        if (isSelected) R.string.builder_chip_selected else R.string.builder_chip_unselected,
        ingredient.name,
    )
    val priceLabel = stringResource(R.string.format_kes, ingredient.pricePerUnitKes.toKesAmount())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            // A border rather than an elevation change: the warm page is flat, so a
            // selected tile has to read as selected from the line alone.
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = RoundedCornerShape(16.dp),
            )
            // Role.Checkbox rather than Role.Button: this is a tick box that happens to
            // look like a card, and TalkBack should say so before the user activates it.
            .clickable(onClickLabel = stateLabel, role = Role.Checkbox, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = "$stateLabel, $priceLabel"
                this.stateDescription = stateLabel
            }
            // Padding on the content, not the Column, so the click target covers the
            // whole tile including the empty space beside a short name.
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = ingredient.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                // Two lines: "Green capsicum" must not truncate at 200% font scale, and
                // the tile has no fixed height for it to clip against.
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Text(
            text = priceLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

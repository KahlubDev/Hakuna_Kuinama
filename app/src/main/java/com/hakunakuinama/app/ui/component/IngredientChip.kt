package com.hakunakuinama.app.ui.component

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.hakunakuinama.app.R
import com.hakunakuinama.app.domain.model.Ingredient
import com.hakunakuinama.app.ui.util.toKesAmount

/**
 * One selectable ingredient in the Menu Builder's chip grid.
 *
 * Accessibility notes, because a 27-chip grid is exactly where a naive implementation
 * fails:
 * - The chip is visually 40dp tall, but `defaultMinSize(minTouchTargetSize = 48.dp)` keeps
 *   the *touch* target at the 48dp minimum. Forcing a 48dp visual height instead would
 *   make the grid absurdly tall on a small screen for no benefit.
 * - The description carries the ingredient, its price and its selected state, so a
 *   screen-reader user does not have to remember what they tapped earlier in the grid.
 *   `stateDescription` is the field TalkBack re-announces when focus returns to the chip,
 *   which is why the state is not baked into `contentDescription` alone.
 */
@Composable
fun IngredientChip(
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

    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = "${ingredient.emoji} ${ingredient.name}",
                style = MaterialTheme.typography.labelLarge,
                // Two lines: "Green capsicum" and "250 g piece"-style names must not
                // truncate to "Green cap…" at 200% font scale.
                maxLines = 2,
            )
        },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = modifier
            .defaultMinSize(minTouchTargetSize = 48.dp)
            .clearAndSetSemantics {
                contentDescription = "$stateLabel, $priceLabel"
                this.stateDescription = stateLabel
            },
    )
}

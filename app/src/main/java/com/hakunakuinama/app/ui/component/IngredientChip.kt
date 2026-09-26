package com.hakunakuinama.app.ui.component

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
 * - The chip is visually ~40dp tall and stays that way. Compose 1.6's Material components
 *   apply `minimumInteractiveComponentSize()` themselves, which expands the *touch* target
 *   to 48dp without changing the layout. Reaching for
 *   `defaultMinSize(minTouchTargetSize = 48.dp)` looks equivalent but that parameter only
 *   exists from Compose 1.7, and it would also make a 27-chip grid absurdly tall.
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
        modifier = modifier.clearAndSetSemantics {
            contentDescription = "$stateLabel, $priceLabel"
            this.stateDescription = stateLabel
        },
    )
}

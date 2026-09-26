package com.hakunakuinama.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hakunakuinama.app.R
import com.hakunakuinama.app.domain.model.Meal
import com.hakunakuinama.app.domain.model.MealMatch
import com.hakunakuinama.app.ui.util.toKesAmount

/**
 * A recipe card: artwork, title, and the cost per plate in KES.
 *
 * The cost comes from [Meal.costPerServingKes] — the value derived from the recipe's own
 * ingredient prices. There is deliberately no cost parameter: a card cannot be handed a
 * number that disagrees with the meal it is displaying.
 *
 * @param match when present, shows how well the recipe fits the Menu Builder's current
 *   selection, so the ranked list reuses this card instead of growing a sibling variant.
 * @param onFavoriteClick null hides the heart entirely, which is what the Menu Builder
 *   wants — a "remove from favorites" control on a list of possible meals is noise.
 */
@Composable
fun MealCard(
    meal: Meal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    match: MealMatch? = null,
    onFavoriteClick: (() -> Unit)? = null,
) {
    val openLabel = stringResource(R.string.action_open_recipe, meal.name)

    Card(
        modifier = modifier
            .fillMaxWidth()
            // Role.Button so a screen reader announces the card as something to activate
            // rather than as a mystery clickable group.
            .clickable(onClickLabel = openLabel, role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                Text(
                    text = meal.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    // Two lines then ellipsis: at 200% font scale an unbounded title
                    // would push the cost badge off the card entirely.
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = meal.tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    CostBadge(costPerServingKes = meal.costPerServingKes)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (match != null) MatchBadge(match)
                        if (onFavoriteClick != null) {
                            IconButton(onClick = onFavoriteClick) {
                                Icon(
                                    imageVector = if (meal.isFavourite) {
                                        Icons.Filled.Favorite
                                    } else {
                                        Icons.Filled.FavoriteBorder
                                    },
                                    contentDescription = stringResource(
                                        if (meal.isFavourite) {
                                            R.string.action_favorite_remove
                                        } else {
                                            R.string.action_favorite_add
                                        },
                                    ),
                                    tint = MaterialTheme.colorScheme.primary,
                                    // IconButton is 48dp by default, so the touch target
                                    // is already accessible without extra padding.
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * "KES 131 · per plate".
 *
 * A [Surface] rather than a Chip on purpose: a disabled chip is not focusable and
 * swallows semantics, which is a lot of trouble for a label that is not interactive.
 */
@Composable
fun CostBadge(
    costPerServingKes: Double,
    modifier: Modifier = Modifier,
) {
    val amount = stringResource(R.string.format_kes, costPerServingKes.toKesAmount())
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Text(
            text = "$amount · ${stringResource(R.string.recipe_per_plate)}",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

/**
 * How well this recipe fits the Menu Builder's selection: "Cook now" when nothing is
 * missing, otherwise a percentage.
 */
@Composable
fun MatchBadge(
    match: MealMatch,
    modifier: Modifier = Modifier,
) {
    val canCookNow = match.canCookNow
    val label = if (canCookNow) {
        stringResource(R.string.match_cook_now)
    } else {
        stringResource(R.string.match_percent, match.matchPercentage)
    }

    Surface(
        modifier = modifier.padding(end = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (canCookNow) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        contentColor = if (canCookNow) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

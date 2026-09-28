package com.hakunakuinama.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hakunakuinama.app.R
import com.hakunakuinama.app.domain.model.Meal
import com.hakunakuinama.app.domain.model.MealMatch
import com.hakunakuinama.app.ui.theme.TealForest
import com.hakunakuinama.app.ui.util.toKesAmount

private val CardShape = RoundedCornerShape(16.dp)

/**
 * A recipe card: artwork, title, and the cost per plate in KES.
 *
 * The cost comes from [Meal.costPerServingKes] — the value derived from the recipe's own
 * ingredient prices. There is deliberately no cost parameter: a card cannot be handed a
 * number that disagrees with the meal it is displaying.
 *
 * @param isHero the one large card at the top of a screen. Takes a 16:10 crop, a Playfair
 *   name and a 16dp text block; every other card is compact with a square crop. The
 *   variant is a parameter rather than a second composable so the Menu Builder, Favorites
 *   and Dashboard all keep sharing one click target, one heart and one cost rule.
 * @param eyebrow optional small-caps kicker above the name — the Dashboard uses it for the
 *   meal slot ("Today's pick for breakfast"), which is the one label that has to come
 *   from the ViewModel's resolved slot rather than from a clock read in composition.
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
    isHero: Boolean = false,
    eyebrow: String? = null,
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
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        // Flat. The warm page behind the card already separates it from the page, and a
        // shadow is the one thing the warm-paper look cannot accommodate.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            // Top corners rounded to match the card, not to 20dp: the card's own outline is
            // 16dp, so a wider radius here would either be clipped away or poke past the
            // card at the corners. Clipped explicitly so the hairline below cannot square
            // the top off.
            Column(
                modifier = Modifier.clip(
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                ),
            ) {
                RecipeImage(
                    imageUrl = meal.imageUrl,
                    contentDescription = meal.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(if (isHero) 16f / 10f else 1f),
                )

                // Hairline under the image. A 1dp Box with only a background carries no
                // semantics of its own, so a screen reader never announces it.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                if (eyebrow != null) {
                    Text(
                        // uppercased here rather than baked into the resource: the string
                        // stays in sentence case for screen readers, which spell out
                        // letter-by-letter text that is stored in capitals. No locale
                        // argument, so it is Locale.ROOT and cannot pick up a Turkish dotless i.
                        text = eyebrow.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    text = meal.name,
                    // displaySmall, not headlineLarge: this scale has no headlineLarge, and
                    // naming one would fall through to the M3 default and set a Playfair
                    // meal name in Roboto.
                    style = if (isHero) {
                        MaterialTheme.typography.displaySmall
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = if (eyebrow != null) {
                        Modifier.padding(top = 6.dp)
                    } else {
                        Modifier
                    },
                    // Two lines then ellipsis: at 200% font scale an unbounded title
                    // would push the cost badge off the card entirely.
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = meal.tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )

                // Nothing to show beyond the text: no trailing row at all, so a plain
                // recipe does not carry an empty strip of padding.
                if (match != null || onFavoriteClick != null) {
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
}

/**
 * "KES 131" — the price alone, on terracotta.
 *
 * The "per plate" words that used to sit inside this pill are gone: every card is priced
 * per serving, so repeating it on each one is noise, and the badge is also read aloud next
 * to a "Cost per plate" row on the recipe screen.
 *
 * White on terracotta is 9.9:1 in light mode and still 5.3:1 in dark mode, where
 * `secondary` becomes the lighter clay, so the fill is fixed rather than paired with
 * `onSecondary` — the paired `onSecondary` only manages 3.5:1 there.
 */
@Composable
fun CostBadge(
    costPerServingKes: Double,
    modifier: Modifier = Modifier,
) {
    val amount = stringResource(R.string.format_kes, costPerServingKes.toKesAmount())
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.secondary,
        contentColor = Color.White,
    ) {
        Text(
            text = amount,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/**
 * How well this recipe fits the Menu Builder's selection: "Cook now" when nothing is
 * missing, otherwise a percentage.
 *
 * Teal on both schemes, fixed at the brand green rather than `primary`. Dark mode's
 * `primary` is the lighter teal, and white text on it drops to 3.7:1 — under the 4.5:1
 * that 11sp text needs. The brand green holds 6.1:1 against white in either scheme.
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
        shape = RoundedCornerShape(4.dp),
        color = TealForest,
        contentColor = Color.White,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

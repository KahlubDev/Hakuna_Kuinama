package com.hakunakuinama.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import com.hakunakuinama.app.ui.theme.TealForest
import com.hakunakuinama.app.ui.util.toKesAmount

/**
 * The banner's proportions, measured off the mockup: 218 × 92. A wide strip of colour, not
 * a photograph-sized block — the design deliberately spends its vertical space on the meal
 * name below rather than on the artwork.
 */
private const val HERO_IMAGE_ASPECT = 2.37f

private val ThumbnailWidth = 84.dp
private val ThumbnailHeight = 66.dp

/**
 * The one large card on the Home screen: a banner of artwork, the meal's name, its price
 * and its tagline.
 *
 * The cost comes from [Meal.costPerServingKes] — the value derived from the recipe's own
 * ingredient prices. There is deliberately no cost parameter: a card cannot be handed a
 * number that disagrees with the meal it is displaying.
 *
 * The small-caps kicker above it ("TODAY'S POCKET-FRIENDLY PICK") belongs to the screen,
 * not the card, so it lives in the Dashboard. The card stays one tappable surface.
 */
@Composable
fun HeroMealCard(
    meal: Meal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val openLabel = stringResource(R.string.action_open_recipe, meal.name)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        // A hairline rather than a shadow: the warm page is flat, and a shadow is the one
        // thing the paper look cannot accommodate. Surface clips its content to `shape`, so
        // the artwork's top corners follow the card's radius without a second clip.
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            // Role.Button so a screen reader announces the card as something to activate
            // rather than as a mystery clickable group.
            modifier = Modifier.clickable(
                onClickLabel = openLabel,
                role = Role.Button,
                onClick = onClick,
            ),
        ) {
            RecipeImage(
                imageUrl = meal.imageUrl,
                contentDescription = meal.name,
                artworkSeed = meal.id,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(HERO_IMAGE_ASPECT),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                // Top, not CentreVertically. In the mockup the badge's cap line sits level
                // with the meal name's, and the tagline hangs below both — centring the
                // badge against the name *and* the tagline would push it down a line and
                // break that alignment.
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = meal.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        // Two lines then ellipsis: at 200% font scale an unbounded title
                        // would push the price badge off the card entirely.
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = meal.tagline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                CostBadge(costPerServingKes = meal.costPerServingKes)
            }
        }
    }
}

/**
 * A compact recipe row: thumbnail on the left, name and tagline beside it, the price badge
 * pinned to the top-right corner and — when asked for — the heart at the bottom-right.
 *
 * One card serves the Home picks, the Menu Builder results and the Favorites list, so all
 * three keep the same click target, spacing and price rule. What differs between them is
 * what goes in the two optional slots:
 *
 * @param footnote a short line under the tagline, aligned with it — the Menu Builder puts
 *   "Cook now" or "5/5 ingredients" there. It sits inside the text column rather than
 *   under the thumbnail, so it lines up with the name above it instead of hanging in the
 *   left margin.
 * @param onFavoriteClick when non-null, shows the heart at the bottom-right corner
 *   (Favorites). Null hides it entirely, which is what the Home picks and the Menu Builder
 *   want — a "remove from favorites" control on a list of possible meals is noise.
 */
@Composable
fun MealRowCard(
    meal: Meal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    footnote: String? = null,
    onFavoriteClick: (() -> Unit)? = null,
) {
    val openLabel = stringResource(R.string.action_open_recipe, meal.name)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = openLabel, role = Role.Button, onClick = onClick)
                .padding(12.dp)
                // Intrinsic height, so the trailing column can span the row: that is how
                // the heart ends up in the bottom corner while the badge stays at the top.
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // The thumbnail is a picture, so it has a fixed size; the text beside it does
            // not, and grows the row instead of clipping at large font scales.
            RecipeImage(
                imageUrl = meal.imageUrl,
                contentDescription = meal.name,
                artworkSeed = meal.id,
                isCompact = true,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .width(ThumbnailWidth)
                    .height(ThumbnailHeight)
                    .clip(MaterialTheme.shapes.small),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
            ) {
                Text(
                    text = meal.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = meal.tagline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (footnote != null) {
                    Text(
                        text = footnote,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                CostBadge(costPerServingKes = meal.costPerServingKes)

                if (onFavoriteClick != null) {
                    // 36dp of layout keeps the card compact; Material still extends the
                    // touch target to 48dp around it, so it stays easy to hit.
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.size(36.dp),
                    ) {
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
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * "KES 131" — the price alone, on the brand green.
 *
 * The "per plate" words that used to sit inside this pill are gone: every card is priced
 * per serving, so repeating it on each one is noise, and the recipe screen says it once.
 *
 * The fill is the fixed brand green rather than `primary`. Dark mode's `primary` is the
 * lighter teal, and white text on it drops under 4.5:1 — the ratio small text needs. The
 * brand green holds 7.5:1 against white in either scheme.
 */
@Composable
fun CostBadge(
    costPerServingKes: Double,
    modifier: Modifier = Modifier,
) {
    val amount = stringResource(R.string.format_kes, costPerServingKes.toKesAmount())
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = TealForest,
        contentColor = Color.White,
    ) {
        Text(
            text = amount,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

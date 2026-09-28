package com.hakunakuinama.app.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hakunakuinama.app.R
import com.hakunakuinama.app.domain.model.Ingredient
import com.hakunakuinama.app.ui.theme.TealForest

/** Ten tiles to a page: five rows of two, which is what fits above the fold on a small phone. */
private const val PAGE_SIZE = 10

/**
 * The kitchen grid: the ingredient catalogue, ten tiles to a page, two to a row, with a
 * pager bar underneath.
 *
 * Paged rather than one long scroll, for the reason the mockups page it: the tiles *are* the
 * screen's job, and a wall of twenty-seven of them pushes the ranked matches — the actual
 * answer the user came for — off the bottom. Ten per page keeps the tiles, the pager and
 * the first couple of results within a screen and a half.
 *
 * The selection is not held here at all: it lives in the ViewModel and is passed in, so
 * ticking something on page one and flipping to page three loses nothing.
 *
 * Staples come first, so the ingredients a student almost certainly has (flour, onions,
 * oil) are on page one and the rarer ones are a flip away. Within that split the
 * catalogue's own aisle order is kept, because that is the order the domain enum declares
 * and no screen has to sort anything.
 *
 * Rows are built by hand instead of with a `LazyVerticalGrid`, because a grid cannot be
 * nested inside a `LazyColumn` item, and this sits in one to keep the screen a single
 * scrolling column. At most ten tiles are ever laid out, so eagerness costs nothing.
 */
@Composable
fun IngredientGrid(
    ingredients: List<Ingredient>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ordered = remember(ingredients) {
        ingredients.sortedWith(
            compareByDescending<Ingredient> { it.isStaple }.thenBy { it.category.ordinal },
        )
    }
    val pageCount = maxOf(1, (ordered.size + PAGE_SIZE - 1) / PAGE_SIZE)

    // Saved so a rotation keeps the page. Coerced on read rather than on write: if the
    // catalogue ever shrinks, a stale page number must not index past the end.
    var page by rememberSaveable { mutableIntStateOf(0) }
    val currentPage = page.coerceIn(0, pageCount - 1)
    val start = currentPage * PAGE_SIZE
    val pageItems = ordered.subList(start, minOf(start + PAGE_SIZE, ordered.size))

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        pageItems.chunked(2).forEachIndexed { rowIndex, row ->
            // Intrinsic minimum height, so a tile whose label wraps to two lines makes its
            // neighbour the same height instead of leaving a ragged pair.
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEachIndexed { columnIndex, ingredient ->
                    IngredientTile(
                        ingredient = ingredient,
                        position = start + rowIndex * 2 + columnIndex + 1,
                        isSelected = ingredient.id in selectedIds,
                        onClick = { onToggle(ingredient.id) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                // An odd last row leaves a hole. An empty weighted Box keeps the last tile
                // at half width rather than stretching it, which would make one ingredient
                // look like the feature of the row.
                if (row.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }

        PantryPager(
            page = currentPage,
            pageCount = pageCount,
            onPrevious = { page = currentPage - 1 },
            onNext = { page = currentPage + 1 },
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/**
 * One tile in the kitchen grid.
 *
 * Unticked it is a paper card with its position in a small outlined box; ticked it fills
 * with the brand green and the box becomes a tick. The fill is the fixed brand teal rather
 * than `primary` — dark mode's lighter `primary` cannot carry white 14sp text at 4.5:1 — so
 * a ticked tile reads the same in both schemes.
 *
 * The description names the ingredient and whether it is ticked, and [stateDescription]
 * carries the tick separately because that is the field TalkBack re-announces on return.
 * The position number and the tick icon are decorative: announcing "5" before every
 * ingredient would make the grid slower to move through, not faster.
 */
@Composable
fun IngredientTile(
    ingredient: Ingredient,
    position: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stateLabel = stringResource(
        if (isSelected) R.string.builder_chip_selected else R.string.builder_chip_unselected,
        ingredient.name,
    )
    val shape = MaterialTheme.shapes.medium
    val fill = if (isSelected) TealForest else MaterialTheme.colorScheme.background
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            // A minimum, not a height: a fixed-height tile clips "Cowpeas (nyama beans)" at
            // 200% font scale, and the tile has nothing else to grow against.
            .heightIn(min = 58.dp)
            .clip(shape)
            .background(fill)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            // Role.Checkbox rather than Role.Button: this is a tick box that happens to
            // look like a card, and TalkBack should say so before the user activates it.
            .clickable(onClickLabel = stateLabel, role = Role.Checkbox, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = stateLabel
                this.stateDescription = stateLabel
            }
            // Padding after clickable, so the touch target covers the whole tile including
            // the empty space beside a short name.
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = ingredient.name,
            style = MaterialTheme.typography.titleSmall,
            color = contentColor,
            // Two lines: a long name must not truncate at large font scales.
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        PositionBox(position = position, isSelected = isSelected)
    }
}

/** The small outlined square at a tile's right: its position, or a tick once chosen. */
@Composable
private fun PositionBox(
    position: Int,
    isSelected: Boolean,
) {
    val stroke = if (isSelected) {
        Color.White.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            // A minimum rather than a fixed size, so a two-digit position at large font
            // scales widens the box instead of overflowing it.
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .border(1.5.dp, stroke, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(13.dp),
            )
        } else {
            Text(
                text = position.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "Pantry essentials · 1 of 3" with the two page buttons. */
@Composable
private fun PantryPager(
    page: Int,
    pageCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        // The paper tone, not the card tone: the bar is a control sitting *on* the page,
        // and a white bar among paper tiles would read as another card.
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 9.dp, end = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.builder_pager_label, page + 1, pageCount),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    // Announce the new page when a button is pressed, so a screen-reader
                    // user who cannot see the tiles change knows the press did something.
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
            PagerButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                labelRes = R.string.builder_page_previous,
                enabled = page > 0,
                onClick = onPrevious,
            )
            Spacer(modifier = Modifier.width(6.dp))
            PagerButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                labelRes = R.string.builder_page_next,
                enabled = page < pageCount - 1,
                onClick = onNext,
            )
        }
    }
}

@Composable
private fun PagerButton(
    icon: ImageVector,
    @StringRes labelRes: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(34.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(labelRes),
                tint = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

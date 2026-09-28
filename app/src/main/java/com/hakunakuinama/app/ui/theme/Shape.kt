package com.hakunakuinama.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The app's corner scale — the one place to change how round things are.
 *
 * The mockups drew everything with square corners, which reads as severe on a phone and
 * makes adjacent cards look like one long slab. This scale softens them by a few
 * millimetres without sliding back into the pill-shaped Material look: at 10dp the
 * hairline borders still read as printed rules rather than as bubbles, and two 10dp
 * cards stacked with 12dp between them stay visibly two cards.
 *
 * Composables never write a `RoundedCornerShape(n.dp)` of their own — they ask the
 * theme for a role. Want it squarer or rounder? Change these five numbers and every
 * screen follows.
 *
 * | Role       | dp | Used for                                                     |
 * |------------|----|--------------------------------------------------------------|
 * | extraSmall | 6  | KES badges, the tile position box, step-duration chips        |
 * | small      | 8  | list-card thumbnails, the pager's arrow buttons, the mark button |
 * | medium     | 10 | the hero card, list cards, pantry tiles, the pager bar, the FAB |
 * | large      | 14 | the recipe detail's top bar backdrop                          |
 * | extraLarge | 20 | the shopping-list sheet's top corners                         |
 */
val HakunaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

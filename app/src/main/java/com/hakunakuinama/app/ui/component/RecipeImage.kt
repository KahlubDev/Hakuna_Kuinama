package com.hakunakuinama.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hakunakuinama.app.ui.theme.ArtworkBrown
import com.hakunakuinama.app.ui.theme.ArtworkStone
import com.hakunakuinama.app.ui.theme.ArtworkTeal

/**
 * A recipe's picture, or the geometric placeholder while there is no picture.
 *
 * The seeded recipes ship with `imageUrl = null`, so the placeholder *is* the artwork
 * today. Both Coil states are wired: `placeholder` for a slow connection, `error` for a
 * dead URL, so the same flat block covers the gap while a real photo loads.
 *
 * The description is set here rather than at the call site so it can never be forgotten
 * on one of the four screens, and so the fallback announces itself sensibly ("Pilau",
 * not "image").
 */
@Composable
fun RecipeImage(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.clearAndSetSemantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl == null) {
            GeometricArtwork()
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = null, // announced by the Box above, once
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                fallback = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}

/**
 * Two overlapping flat-colour rectangles — the design placeholder while real recipe
 * photography is pending. Deliberately has no gradient, no emoji, no dynamic colour: the
 * two blocks plus the scissors icon read as a brand mark rather than a loading state.
 *
 * Colours come from the [ArtworkTeal] / [ArtworkBrown] / [ArtworkStone] palette values
 * rather than the colour scheme, so the artwork looks the same in light and dark mode —
 * real photos will do the same. That is also why it does not follow dynamic colour: a
 * photograph placeholder that re-tints with the user's wallpaper is a mood ring, not a
 * recipe.
 *
 * The blocks are fixed-size by design. This is a *picture*, so the minimum-height rule
 * that protects text from clipping at 200% font scale does not apply to it.
 */
@Composable
fun GeometricArtwork(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ArtworkStone),
    ) {
        // Large teal block — top-left, ~65% width, full height
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.65f)
                .align(Alignment.TopStart)
                .background(ArtworkTeal),
        )
        // Small brown square — bottom-left, ~32% width, ~45% height
        Box(
            modifier = Modifier
                .fillMaxHeight(0.45f)
                .fillMaxWidth(0.32f)
                .align(Alignment.BottomStart)
                .background(ArtworkBrown),
        )
        // Scissors icon — centered, white at 60% alpha
        Icon(
            imageVector = Icons.Outlined.ContentCut,
            contentDescription = null, // decorative
            tint = Color.White.copy(alpha = 0.60f),
            modifier = Modifier
                .size(28.dp)
                .align(Alignment.Center),
        )
    }
}

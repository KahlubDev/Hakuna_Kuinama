package com.hakunakuinama.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * A recipe's picture, or the geometric placeholder while there is no picture.
 *
 * The seeded recipes ship with `imageUrl = null`, so the placeholder *is* the artwork
 * today. Both Coil states are wired: `placeholder` for a slow connection, `error` for a
 * dead URL, so the same flat block covers the gap while a real photo loads.
 *
 * @param artworkSeed picks the placeholder's colourway. Pass the meal's id and every recipe
 *   keeps its own colours from one launch to the next, instead of five identical green
 *   rectangles in a list.
 * @param isCompact true for the small thumbnail on a list card, which needs its own
 *   composition — the banner's tilted block and corner bracket shrink to noise at 86dp, so
 *   the thumbnail is a two-tone split with a hairline stripe down its left edge.
 *
 * The description is set here rather than at the call site so it can never be forgotten on
 * one of the four screens, and so the fallback announces itself sensibly ("Pilau", not
 * "image").
 */
@Composable
fun RecipeImage(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    artworkSeed: Long = 0L,
    isCompact: Boolean = false,
) {
    Box(
        modifier = modifier.clearAndSetSemantics { this.contentDescription = contentDescription },
    ) {
        if (imageUrl == null) {
            GeometricArtwork(seed = artworkSeed, isCompact = isCompact)
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

/** One placeholder colourway: the ground, the tilted block, and the hairline frame. */
@Immutable
private data class ArtworkPalette(
    val ground: Color,
    val block: Color,
    val frame: Color,
)

/**
 * Sampled from the mockups: the first three are the colourways the design actually uses on
 * its recipe cards, the fourth is a deeper terracotta-green so a fifth recipe does not
 * repeat the first.
 *
 * Fixed colours rather than colour-scheme roles. The artwork stands in for a photograph,
 * and a photograph looks the same in light and dark mode — and does not re-tint with the
 * user's wallpaper. That is also why it does not follow dynamic colour: a placeholder that
 * changes with the home screen is a mood ring, not a recipe.
 */
private val ArtworkPalettes = listOf(
    ArtworkPalette(Color(0xFF628F83), Color(0xFF786E5D), Color(0xFF989D88)),
    ArtworkPalette(Color(0xFF789B8F), Color(0xFF837463), Color(0xFFA5A48F)),
    ArtworkPalette(Color(0xFFA9B99A), Color(0xFF9B8369), Color(0xFFC2B695)),
    ArtworkPalette(Color(0xFF456F68), Color(0xFF695E50), Color(0xFF7F9A8F)),
)

/** The cutlery mark, in the pale cream the mockups use over the block. */
private val ArtworkIconTint = Color(0xFFEFE4D2)

/**
 * The design placeholder while real recipe photography is pending: a flat ground, a tilted
 * block, a hairline corner bracket and the cutlery mark. No gradient, no emoji, no dynamic
 * colour — it reads as a brand mark rather than a loading state.
 *
 * Drawn in proportions of its own bounds rather than at fixed sizes, so the same code fills
 * the full-bleed banner on the recipe screen, the hero card on Home, and an 86dp thumbnail
 * on a list card. It is a *picture*, so the minimum-height rule that protects text from
 * clipping at 200% font scale does not apply to it.
 */
@Composable
fun GeometricArtwork(
    seed: Long,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
) {
    val count = ArtworkPalettes.size.toLong()
    // The double modulo keeps a negative id (a Room autoincrement never is, but a test
    // fixture might be) from indexing off the front of the list.
    val palette = ArtworkPalettes[(((seed % count) + count) % count).toInt()]

    Box(
        modifier = modifier
            .fillMaxSize()
            // The tilted block and the bracket are both drawn running off their edges, so
            // the canvas has to clip or the overshoot paints over the card below.
            .clipToBounds()
            .background(palette.ground),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (isCompact) drawThumbnail(palette) else drawBanner(palette)
        }
        // After the Canvas, so it sits on top of the block the way it does in the mockup.
        Icon(
            imageVector = Icons.Outlined.Restaurant,
            contentDescription = null, // decorative; the Box announces the meal's name
            tint = ArtworkIconTint,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = if (isCompact) 7.dp else 14.dp, bottom = if (isCompact) 6.dp else 10.dp)
                .size(if (isCompact) 15.dp else 21.dp),
        )
    }
}

/**
 * The wide composition: a hairline bracket opening towards the top-right, and a block
 * tilted about 10° that runs off the bottom edge.
 *
 * The fractions are measured off the mockup's banner, not eyeballed: the bracket's upright
 * is 4.5% of the width and starts 75% across, its corner is 57% of the way down, and the
 * block's top-left corner sits at 9% × 37% with a width of 29.5%. The mockups draw the
 * artwork at 242 × 92 and 218 × 92, so a ratio-based composition is the only way one piece
 * of code can fill both without distorting either.
 */
private fun DrawScope.drawBanner(palette: ArtworkPalette) {
    val w = size.width
    val h = size.height

    // The bracket. The horizontal arm bleeds off the right edge, which is what makes it
    // read as a corner rather than as a box.
    val arm = w * 0.045f
    val cornerX = w * 0.75f
    val cornerY = h * 0.57f
    drawRect(palette.frame, topLeft = Offset(cornerX, 0f), size = Size(arm, cornerY))
    drawRect(
        palette.frame,
        topLeft = Offset(cornerX, cornerY - arm),
        size = Size(w - cornerX, arm),
    )

    // The tilted block. Rotating about its own top-left corner keeps the corner where the
    // composition is anchored and throws the rest of the square off the bottom, so the
    // height is deliberately over-generous.
    val hinge = Offset(w * 0.09f, h * 0.37f)
    rotate(degrees = 10.5f, pivot = hinge) {
        drawRect(palette.block, topLeft = hinge, size = Size(w * 0.295f, h * 2f))
    }
}


/**
 * The thumbnail composition: a hairline stripe down the left edge, then the block filling
 * the rest with a leaning left edge. Simpler than the banner because at 86dp wide the
 * bracket's arms are thinner than a pixel and the tilt is invisible.
 */
private fun DrawScope.drawThumbnail(palette: ArtworkPalette) {
    val w = size.width
    val h = size.height

    drawRect(palette.frame, topLeft = Offset.Zero, size = Size(w * 0.05f, h))
    // A quadrilateral rather than a rotated rect: the lean has to be visible in the
    // silhouette, and a rotation this small is lost to rounding at this size.
    val block = Path().apply {
        moveTo(w * 0.34f, 0f)
        lineTo(w, 0f)
        lineTo(w, h)
        lineTo(w * 0.13f, h)
        close()
    }
    drawPath(block, palette.block)
}

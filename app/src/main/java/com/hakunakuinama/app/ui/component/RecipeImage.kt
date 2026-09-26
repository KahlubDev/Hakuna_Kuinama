package com.hakunakuinama.app.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.hakunakuinama.app.R

/**
 * A recipe's picture, or its emoji when there is no picture.
 *
 * The seeded recipes ship with `imageUrl = null` and an emoji, so the emoji *is* the
 * artwork today. Rendering an empty grey box instead would look like a bug; rendering
 * the emoji makes the catalogue look intentional. When real photos are added, the Coil
 * branch takes over and the same placeholder covers the gap while it loads.
 *
 * Both Coil states are wired: `placeholder` for a slow connection, `error` for a dead
 * URL. A cross-fade keeps the swap from flickering when the image arrives.
 */
@Composable
fun RecipeImage(
    imageUrl: String?,
    emoji: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    // The description is set here rather than at the call site so it can never be
    // forgotten on one of the four screens, and so the fallback announces itself
    // sensibly ("Pilau", not "photo of Pilau" over an emoji).
    val description = if (imageUrl == null) contentDescription else "$contentDescription. $emoji"

    Box(
        modifier = modifier.clearAndSetSemantics { this.contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl == null) {
            EmojiArtwork(emoji = emoji)
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
 * Emoji on a tinted square. Uses a colour derived from the theme rather than a hard-coded
 * one, so it stays legible in both schemes and follows dynamic colour if it is ever
 * switched on.
 */
@Composable
fun EmojiArtwork(
    emoji: String,
    modifier: Modifier = Modifier,
) {
    Crossfade(targetState = emoji, label = "emojiArtwork") { value ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                // Fixed size, not sp-driven: an emoji is a picture, not text, and it
                // should not balloon at 200% font scale and overflow its card.
                modifier = Modifier.size(56.dp),
                color = Color.Unspecified,
            )
        }
    }
}

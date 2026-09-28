package com.hakunakuinama.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The single loading / empty / error surface for the whole app.
 *
 * One component rather than four per-screen variants, for the boring reason that they
 * drift: three of them end up with different padding and only one gets fixed when the
 * text wraps at 200% font scale. Every screen routes its non-content state through this,
 * passing different copy.
 *
 * @param isLoading swaps the icon for a spinner, so the spinner is never re-implemented
 *   per screen either.
 * @param action optional recovery affordance (a retry button, a "clear list" button).
 *   Null simply renders no affordance.
 */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            // The horizontal padding is tighter than the 20dp gutter the content uses: a
            // centred short message with a full-width measure sets into ragged margins and
            // reads as narrower than it is.
            .padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // The live region wraps the icon/title/body but *not* the action. Applying
        // clearAndSetSemantics to a Column that also contains a button clears that
        // button's semantics too, which would make the recovery affordance unreachable by
        // TalkBack — the one case where a screen-reader user most needs it.
        Column(
            modifier = Modifier.clearAndSetSemantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "$title. $body"
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    // Decorative here: the title and body already say it, and a screen
                    // reader announcing "Information" before the sentence is noise.
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (icon != null || isLoading) 16.dp else 0.dp),
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }

        if (action != null) {
            Column(modifier = Modifier.padding(top = 24.dp)) { action() }
        }
    }
}

/**
 * A bordered panel holding an [EmptyState] inside a `LazyColumn` item.
 *
 * The loading and prompt states on the Menu Builder are the awkward case: `EmptyState`
 * fills its parent, and a `LazyColumn` item has an unbounded height, so without a bound
 * there is nothing to lay out against and the content collapses or throws. Every one of
 * those call sites needs the same fix, so it lives here rather than being repeated as a
 * `heightIn` at each.
 */
@Composable
fun BorderedEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    minHeight: Dp = 200.dp,
    action: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        EmptyState(
            title = title,
            body = body,
            icon = icon,
            isLoading = isLoading,
            // A *minimum*, not a fixed height: at 200% font scale a taller title and body
            // have to be able to grow instead of being clipped.
            modifier = Modifier.heightIn(min = minHeight),
            action = action,
        )
    }
}

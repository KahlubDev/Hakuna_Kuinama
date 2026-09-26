package com.hakunakuinama.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
            .padding(horizontal = 32.dp, vertical = 24.dp)
            // One live region for the whole block: a screen reader announces the state
            // once instead of reading the icon, then the title, then the body separately.
            .clearAndSetSemantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "$title. $body"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(40.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                // The icon is decorative here: the title and body already say it, and a
                // screen reader announcing "Information" before the sentence is noise.
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
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
            modifier = Modifier.padding(top = 8.dp),
        )

        if (action != null) {
            Column(modifier = Modifier.padding(top = 24.dp)) { action() }
        }
    }
}

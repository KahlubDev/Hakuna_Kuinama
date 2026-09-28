package com.hakunakuinama.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * The space above a screen's header, below the status-bar inset.
 *
 * One constant, because the three top-level screens are flipped between constantly and
 * the top edge is the first thing the eye compares between them. Each screen passing its
 * own value is how they end up four pixels apart — which reads as a flicker, not as three
 * different screens.
 *
 * The Scaffold has already consumed the status-bar inset by the time a screen's content
 * padding arrives, so this is measured from the bottom of the status bar, not from the top
 * of the display.
 */
val ScreenTopPadding = 18.dp

/** The gutter the three top-level screens share: headers, cards and section rules. */
val ScreenGutter = 20.dp

/**
 * The opening block of a top-level screen: a small-caps kicker, a large title, and an
 * optional second line.
 *
 * Home, the Menu Builder and Favorites all open the same way, so the spacing lives here
 * once. That matters for the reason the mockups are drawn this way: the three tabs get
 * flipped between constantly, and a title that sits four pixels higher on one tab reads
 * as a flicker rather than as three screens.
 *
 * @param subtitle a second line under the title.
 * @param isSubtitleTitleSized when true the subtitle repeats the title's size in the
 *   eyebrow brown — the Menu Builder's "What's in your kitchen? / Nini jikoni kwako?" pair
 *   is one question asked in two languages, so it is set as one block. When false it is a
 *   plain line of secondary text, as under "Saved meals".
 * @param trailing an optional control pinned to the top-right, level with the title rather
 *   than with the kicker. The Home screen's cutlery mark sits there.
 */
@Composable
fun ScreenHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isSubtitleTitleSized: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                // Uppercased here rather than baked into the resource: the string stays in
                // sentence case for screen readers, which spell out letter-by-letter text
                // that is stored in capitals. No locale argument, so it is Locale.ROOT and
                // cannot pick up a Turkish dotless i.
                text = eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .semantics { heading() },
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = if (isSubtitleTitleSized) {
                        MaterialTheme.typography.headlineMedium
                    } else {
                        MaterialTheme.typography.bodyLarge
                    },
                    color = if (isSubtitleTitleSized) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    // No top gap when the subtitle is title-sized: the two lines are one
                    // block, and a gap between them would split the sentence in two.
                    modifier = Modifier.padding(top = if (isSubtitleTitleSized) 0.dp else 6.dp),
                )
            }
        }
        if (trailing != null) {
            // Offset by the kicker's line height plus the gap, so the control lines up with
            // the title instead of floating up beside the eyebrow.
            Box(modifier = Modifier.padding(top = 23.dp)) { trailing() }
        }
    }
}

/**
 * A section heading with a small item pinned to its right: "This week's picks" beside
 * "Build a meal ›", or "Best matches" beside "cheapest first".
 *
 * The trailing slot is a [RowScope] so the caller can put `Modifier.alignByBaseline()` on
 * its own content. That is the whole point of this composable existing: a 21sp heading and
 * a 13sp link have very different line boxes, and centring or bottom-aligning them leaves
 * the small item visibly detached from the line it belongs to. Sharing a baseline is the
 * only alignment that survives a font-scale change.
 */
@Composable
fun SectionHeaderRow(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        // Bottom, not Baseline: the row needs a height before either child's baseline is
        // known, and the real baseline alignment is opted into per child below.
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .alignByBaseline()
                .semantics { heading() },
        )
        // Invoked as a direct child of the Row, never wrapped in a Box. `alignByBaseline()`
        // works by attaching parent data to the modifier chain the Row measures, and an
        // intervening Box swallows it — the trailing item would silently fall back to
        // bottom alignment, which is the exact misalignment this component exists to stop.
        if (trailing != null) {
            trailing()
        }
    }
}

/**
 * A tappable line of text in the accent colour, for the right-hand slot of a
 * [SectionHeaderRow]: "Build a meal ›".
 *
 * Its own component because the accessibility work is easy to get wrong and easy to lose:
 * a bare `Text` with a `clickable` is focusable but is announced as a paragraph, not as a
 * button, and it gets no click label. Here the whole thing is one node with
 * [Role.Button] and the caller's action as its label, and the whole line is the target —
 * a 13sp line of text is not comfortably hittable on its own glyphs.
 *
 * @param onClickLabel what the action *does*, spoken instead of the visible words. "Build a
 *   menu" rather than "Build a meal", because a screen reader user has not seen the words
 *   and needs the destination.
 */
@Composable
fun TextLink(
    text: String,
    onClickLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
            // Vertical padding to reach a 48dp touch target: the line is 18dp tall, and a
            // student on a bus should not have to aim.
            .padding(vertical = 15.dp),
    )
}


package com.hakunakuinama.app.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.hakunakuinama.app.R

/**
 * One numbered instruction in a recipe's method.
 *
 * The number is a bare teal numeral rather than a filled circle. The rest of the design is
 * drawn with hairlines and flat colour — a solid disc every 40dp would be the only
 * shadowed-looking thing on the page — and a numeral set in the accent colour still reads
 * as a step marker, more quietly.
 *
 * The whole row is a single merged semantics node, so a screen reader announces
 * "Step 3, 5 min. Fry the chicken until golden" as one utterance. Left unmerged, the user
 * would hear a bare "3", a pause, then the sentence, then "3" again from the numeral.
 */
@Composable
fun StepItem(
    stepNumber: Int,
    instruction: String,
    modifier: Modifier = Modifier,
    durationMinutes: Int? = null,
) {
    val stepLabel = stringResource(R.string.recipe_step_number, stepNumber)
    val durationLabel = durationMinutes?.let { stringResource(R.string.recipe_step_duration, it) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = buildString {
                    append(stepLabel)
                    durationLabel?.let { append(", $it") }
                    append(". $instruction")
                }
            },
        verticalAlignment = Alignment.Top,
    ) {
        // Decorative: the number is already the first word of the row's description, so
        // announcing it separately would just be noise. A fixed width rather than a fixed
        // height, so a two-digit step at 200% font scale widens the gutter instead of
        // colliding with the text.
        Box(
            modifier = Modifier.size(width = 26.dp, height = 24.dp),
            contentAlignment = Alignment.TopStart,
        ) {
            Text(
                text = stepNumber.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Column(modifier = Modifier.padding(start = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stepLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (durationLabel != null) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Text(
                            text = durationLabel,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text(
                text = instruction,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * The running header above a numbered method: a small teal eyebrow and the section title.
 *
 * Separate from [StepItem] so the header does not scroll away with the first step, which is
 * what happens when the pair is one list item.
 */
@Composable
fun MethodHeader(
    title: String,
    modifier: Modifier = Modifier,
    stepCount: Int? = null,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (stepCount != null) {
            Text(
                text = "  ·  ",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = stepCount.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** A short, muted note under a section — "Optional ingredients are not added…". */
@Composable
fun Footnote(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

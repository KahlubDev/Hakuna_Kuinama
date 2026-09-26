package com.hakunakuinama.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * The whole row is a single merged semantics node, so a screen reader announces
 * "Step 3, 5 min. Fry the chicken until golden" as one utterance. Left unmerged, the user
 * would hear a bare "3", a pause, then the sentence, then "3" again from the badge.
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
        // The badge is decorative: its number is already the first word of the row's
        // description, so announcing it separately would just be noise.
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stepNumber.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }

        Column(modifier = Modifier.padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stepLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (durationLabel != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Text(
                            text = durationLabel,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text(
                text = instruction,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

package com.hakunakuinama.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hakunakuinama.app.R
import com.hakunakuinama.app.domain.usecase.MealSuggestion
import com.hakunakuinama.app.ui.component.BorderedEmptyState
import com.hakunakuinama.app.ui.component.EmptyState
import com.hakunakuinama.app.ui.component.HeroMealCard
import com.hakunakuinama.app.ui.component.MealRowCard
import com.hakunakuinama.app.ui.component.ScreenGutter
import com.hakunakuinama.app.ui.component.ScreenHeader
import com.hakunakuinama.app.ui.component.ScreenTopPadding
import com.hakunakuinama.app.ui.component.SectionHeaderRow
import com.hakunakuinama.app.ui.util.toDateline
import com.hakunakuinama.app.ui.viewmodel.DashboardUiState

/**
 * The dashboard: the day, a greeting, today's pick, and the rest of the week.
 *
 * Stateless on purpose — it takes a [DashboardUiState] and emits callbacks, so it can be
 * previewed and screenshot-tested without Hilt, a ViewModel or a database. The route
 * composable next to it does the wiring.
 */
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onOpenRecipe: (Long) -> Unit,
    onOpenBuilder: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    when (val state = uiState) {
        DashboardUiState.Loading -> EmptyState(
            title = stringResource(R.string.state_loading),
            body = stringResource(R.string.state_loading_body),
            isLoading = true,
            modifier = modifier,
        )

        is DashboardUiState.Error -> EmptyState(
            title = stringResource(R.string.state_error_title),
            body = stringResource(R.string.state_error_body),
            icon = Icons.Filled.Warning,
            modifier = modifier,
        )

        is DashboardUiState.Ready -> ReadyContent(
            state = state,
            onOpenRecipe = onOpenRecipe,
            onOpenBuilder = onOpenBuilder,
            modifier = modifier,
            contentPadding = contentPadding,
        )
    }
}

@Composable
private fun ReadyContent(
    state: DashboardUiState.Ready,
    onOpenRecipe: (Long) -> Unit,
    onOpenBuilder: () -> Unit,
    modifier: Modifier,
    contentPadding: PaddingValues,
) {
    val today = state.suggestion.meal
    // The hero is already on screen, so it is not repeated in the list. Falling back to the
    // whole list keeps the section populated if the catalogue only holds the suggested meal
    // — an empty heading under a full card reads as a bug.
    val picks = remember(state.picks, today?.id) {
        state.picks.filterNot { today != null && it.id == today.id }
            .ifEmpty { state.picks }
    }
    // The FAB is the one control on this screen, so it is worth its own row of scroll at
    // the bottom rather than a translucent scrim sitting on top of the last card's price.
    val hasPicks = picks.isNotEmpty()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                ScreenHeader(
                    // The weekday, not a clock. The greeting below already carries the
                    // time of day, and both come from the state — resolved once from the
                    // injected Clock — so they cannot disagree at a slot boundary.
                    eyebrow = state.today.toDateline(),
                    title = stringResource(state.greeting.labelRes),
                    modifier = Modifier.padding(
                        start = ScreenGutter,
                        end = ScreenGutter,
                        top = ScreenTopPadding,
                        bottom = 10.dp,
                    ),
                )
            }

            item(key = "hero-eyebrow") {
                Text(
                    text = stringResource(R.string.dashboard_pocket_friendly_pick).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = ScreenGutter, vertical = 4.dp),
                )
            }

            item(key = "hero") {
                SuggestedMealHero(
                    suggestion = state.suggestion,
                    onOpenRecipe = onOpenRecipe,
                    onOpenBuilder = onOpenBuilder,
                    modifier = Modifier.padding(horizontal = ScreenGutter),
                )
            }

            if (hasPicks) {
                item(key = "picks-header") {
                    SectionHeaderRow(
                        title = stringResource(R.string.dashboard_this_weeks_picks),
                        modifier = Modifier.padding(
                            start = ScreenGutter,
                            end = ScreenGutter,
                            top = 12.dp,
                            bottom = 2.dp,
                        ),
                    )
                }

                items(items = picks, key = { it.id }) { meal ->
                    MealRowCard(
                        meal = meal,
                        onClick = { onOpenRecipe(meal.id) },
                        modifier = Modifier.padding(horizontal = ScreenGutter),
                    )
                }
            }

            // Room for the FAB plus the bottom inset, so the last card is never trapped
            // under it and the list can always be scrolled clear.
            item(key = "fab-spacer") {
                Box(modifier = Modifier.height(if (hasPicks) 96.dp else 24.dp))
            }
        }

        // The Menu Builder is a *tab*, so the bar already reaches it. This is a shortcut,
        // not the only way there — and putting it on the FAB rather than inline in the
        // "This week's picks" heading keeps the section heading a heading. A teal link
        // wedged into a heading line reads as part of the title, and it is the one control
        // on the screen that has to compete with the greeting for attention.
        ExtendedFloatingActionButton(
            onClick = onOpenBuilder,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.RestaurantMenu,
                    contentDescription = null,
                )
            },
            text = { Text(stringResource(R.string.dashboard_build_a_meal)) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(ScreenGutter),
        )
    }
}

/**
 * The hero card: the meal the ViewModel picked for this slot.
 *
 * The slot label is deliberately **not** shown. The design's kicker above the card is a
 * fixed "TODAY'S POCKET-FRIENDLY PICK" line, and the greeting above that already says what
 * time of day it is. Re-deriving the time of day in composition would also be the one thing
 * that could disagree with the already-selected meal at a slot boundary, so the UI takes
 * the meal exactly as [MealSuggestion] bundled it and reads no clock.
 */
@Composable
private fun SuggestedMealHero(
    suggestion: MealSuggestion,
    onOpenRecipe: (Long) -> Unit,
    onOpenBuilder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val meal = suggestion.meal

    if (meal == null) {
        // No recipe for this slot yet. An empty state, not an error: the catalogue is
        // still warming up on first launch.
        BorderedEmptyState(
            title = stringResource(R.string.dashboard_no_suggestion_title),
            body = stringResource(R.string.dashboard_no_suggestion_body),
            icon = Icons.Filled.Info,
            // The copy says "check back in a moment", which on its own is a dead end: the
            // user is told to wait with nothing to do while they wait. The Builder is the
            // one screen that can make a suggestion appear, so it goes here rather than
            // leaving the state with no way forward.
            action = {
                FilledTonalButton(onClick = onOpenBuilder) {
                    Text(text = stringResource(R.string.action_build_menu))
                }
            },
            minHeight = 210.dp,
            modifier = modifier,
        )
        return
    }

    HeroMealCard(
        meal = meal,
        onClick = { onOpenRecipe(meal.id) },
        modifier = modifier,
    )
}

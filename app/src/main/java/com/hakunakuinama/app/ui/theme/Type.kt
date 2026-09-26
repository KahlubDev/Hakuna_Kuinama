package com.hakunakuinama.app.ui.theme

import androidx.compose.material3.Typography

/**
 * Material 3's default type scale, used as-is.
 *
 * Deliberate: every style is expressed in `sp`, so the whole app scales with the
 * system font-size setting. The layouts that matter (recipe cards, chips, the grocery
 * list) avoid fixed heights for the same reason — at 200% font scale a fixed-height row
 * clips its own text, and clipping the ingredient list of a poor student's dinner is
 * not an acceptable failure mode.
 *
 * Swap in a custom scale here when there is a brand decision to make; nothing else in
 * the app hard-codes a text style.
 */
val HakunaTypography = Typography()

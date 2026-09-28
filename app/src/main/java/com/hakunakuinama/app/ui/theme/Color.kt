package com.hakunakuinama.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ── Palette ──────────────────────────────────────────────────────────────────
//
// Every value below was sampled out of the editorial mockups rather than picked by eye,
// so the app and the design are the same colours and not two similar ones. The
// hexadecimal comments are the sampled value; where one is darkened, the comment says so
// and the reason is a contrast ratio, not taste.
//
// **The only colours a composable is allowed to name are the two brand ones at the top of
// this file.** Everything else is a `ColorScheme` role, which is what makes the app dark
// mode work: a composable that writes `Color(0xFFFFFDF9)` renders a white card on a black
// page and nobody notices until they switch their phone to dark at nine at night.
//
// A composable that must use a fixed colour uses `brandGreen`, and only for one thing: a
// surface that carries **white** text. Dark mode's `primary` is the lighter teal, and
// white on it manages 3.7:1 — under the 4.5:1 small text needs. So a KES badge and a
// ticked pantry tile keep the brand green in both schemes, and everything around them
// follows the scheme.

// Page background — the warm paper everything else sits on. The darkest surface in the
// light scheme: the page is what recedes, the cards sit above it.
val WarmPaper = Color(0xFFF5F0EB)
val WarmPaperDark = Color(0xFF14110E)

/**
 * The brand green. Sampled #315C54 off the KES badge and the selected pantry tiles.
 *
 * The single accent of the app: badges, selected tiles, prices, links, the selected nav
 * item, and the FAB. Fixed in both schemes because it is always paired with white text —
 * see the note at the top of this file.
 */
val TealForest = Color(0xFF315C54)

/** The same green, lifted for dark mode. Carries dark text, never white. 8.9:1 on #14110E. */
val TealForestLight = Color(0xFF7FB3A2)

val TealContainer = Color(0xFFDCEAE4)
val OnTealContainer = Color(0xFF0D2A22)

// Eyebrow brown. Sampled #A48677 off the "TUESDAY" kickers, then darkened twice over: the
// sampled value manages 2.97:1 on the paper, and even #8A6E5F manages only 4.14:1 — both
// under the 4.5:1 that an 11sp letter-spaced small-caps line needs. #7D6353 is the value
// that clears it (4.9:1 on the paper, 5.5:1 on a card) while staying in the same hue.
val EyebrowBrown = Color(0xFF7D6353)
val EyebrowBrownLight = Color(0xFFC4AE9E)

// Text. Sampled #241B16 for the light ink — the mockup's headings are not pure black.
val Ink = Color(0xFF241B16)
val InkMuted = Color(0xFF6F645E)
val InkLight = Color(0xFFEDE5DC)

// Terracotta — reserved for the few places a second hue is needed.
val Terra = Color(0xFF5C3B2E)
val TerraLight = Color(0xFFB08A79)
val TerraContainer = Color(0xFFF2DDD5)
val OnTerraContainer = Color(0xFF23100A)

// ── Light scheme ─────────────────────────────────────────────────────────────
//
// Four surfaces, lightest first, and the mockups draw exactly four: the page, the cards,
// the bar, and the recessed control fills. #FFFDF9 and #FEFCF9 are a hair apart in the
// design and are two values here, because collapsing them flattens a distinction the
// layout is relying on.

private val LightPaper = Color(0xFFF5F0EB) // page
private val LightCard = Color(0xFFFFFDF9) // cards, the FAB's neighbour
private val LightNavSurface = Color(0xFFFEFCF9) // the bottom bar
private val LightRecessed = Color(0xFFEEE8E1) // the pager bar, step chips
// Sampled #DED1C6, nudged a shade darker to #D2C2B4. At the sampled value the outline of
// a card manages 1.32:1 against the page, and a hairline is the *only* thing separating a
// card from the background — the two surfaces themselves differ by 1.11:1, because that is
// what the design draws. Darkening the rule lifts it to 1.53:1 so the card has an edge
// without gaining a shadow it is not supposed to have.
private val LightHairline = Color(0xFFD2C2B4) // card outlines, the bar's top rule

val LightColors = lightColorScheme(
    primary = TealForest,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = OnTealContainer,
    secondary = Terra,
    onSecondary = Color.White,
    secondaryContainer = TerraContainer,
    onSecondaryContainer = OnTerraContainer,
    // The eyebrow kickers and the Swahili subtitle lines.
    tertiary = EyebrowBrown,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF2E7DE),
    onTertiaryContainer = Ink,
    background = LightPaper,
    onBackground = Ink,
    surface = LightCard,
    onSurface = Ink,
    // The bottom bar. Material has no "navigation bar" role, so it borrows
    // `surfaceContainer`, which is exactly what that role is for: a surface that sits
    // above `surface` without a shadow.
    surfaceContainer = LightNavSurface,
    surfaceVariant = LightRecessed,
    onSurfaceVariant = InkMuted,
    // The ingredient table's row rules. One step lighter than the card outlines, as the
    // mockups draw them: inside a card a rule should not read as a border.
    surfaceContainerHighest = LightRecessed,
    outline = Color(0xFFC8B8A8),
    outlineVariant = LightHairline,
    scrim = Color.Black,
    inverseSurface = Color(0xFF302820),
    inverseOnSurface = InkLight,
    inversePrimary = TealForestLight,
    surfaceTint = TealForest,
)

// ── Dark scheme ──────────────────────────────────────────────────────────────
//
// The same four-surface structure, inverted and warmed: the page is the darkest, the
// cards sit one step above it, the bar one step above that, and the recessed fills one
// step above that. The hues are the light scheme's rotated around the same warm neutral,
// so a recipe card looks like the same object in both modes rather than a different one.

private val DarkPaper = Color(0xFF14110E) // page
private val DarkCard = Color(0xFF1F1A16) // cards
private val DarkNavSurface = Color(0xFF262019) // the bottom bar
private val DarkRecessed = Color(0xFF332C25) // the pager bar, step chips
private val DarkHairline = Color(0xFF433A31) // card outlines, the bar's top rule

val DarkColors = darkColorScheme(
    primary = TealForestLight,
    onPrimary = Color(0xFF0A1F1A),
    primaryContainer = Color(0xFF2A4038),
    onPrimaryContainer = TealContainer,
    secondary = TerraLight,
    onSecondary = Color(0xFF23100A),
    secondaryContainer = Color(0xFF3D2C24),
    onSecondaryContainer = TerraContainer,
    tertiary = EyebrowBrownLight,
    onTertiary = Color(0xFF2C1A10),
    tertiaryContainer = Color(0xFF3A2A20),
    onTertiaryContainer = InkLight,
    background = DarkPaper,
    onBackground = InkLight,
    surface = DarkCard,
    onSurface = InkLight,
    surfaceContainer = DarkNavSurface,
    surfaceVariant = DarkRecessed,
    onSurfaceVariant = Color(0xFFBCAEA2),
    surfaceContainerHighest = DarkRecessed,
    outline = Color(0xFF6B5C50),
    outlineVariant = DarkHairline,
    scrim = Color.Black,
    inverseSurface = InkLight,
    inverseOnSurface = Color(0xFF302820),
    inversePrimary = TealForest,
    surfaceTint = TealForestLight,
)

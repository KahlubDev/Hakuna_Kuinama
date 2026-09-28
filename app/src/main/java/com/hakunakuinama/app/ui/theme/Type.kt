package com.hakunakuinama.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hakunakuinama.app.R

/**
 * The app's type scale: Playfair Display for anything that is a *name*, DM Sans for
 * anything that is *information*.
 *
 * Two families, not one. A recipe is a thing you look at, so its name, the screen
 * titles and the KES figures are set in a transitional serif — a plate of ugali and
 * sukuma wiki is a photograph, not a line item. Everything the user has to *act* on
 * (chips, ingredient rows, nav labels, badges) is DM Sans, because a sans at 11sp
 * stays legible where a serif turns to mud.
 *
 * The fonts are **bundled**, not fetched. `ui-text-google-fonts` would resolve these
 * through Google Play Services, which means a silent fall back to the system default
 * on any device without Play Services, and a possible network fetch on first render.
 * This app promises that nothing leaves the device, so the TTFs ship in
 * `res/font/` (OFL, ~755 KB) and the promise holds on a de-Googled handset too.
 *
 * Static instances, not the variable originals. Upstream ships `PlayfairDisplay[wght]`
 * and `DMSans[opsz,wght]`; the six files in `res/font/` were instanced to fixed weights
 * so there is no per-instance rasterisation cost at list scroll. DM Sans is pinned to
 * `opsz=14`, its text optical size — at 13-15sp the display cut of that family is too
 * loose. The OFL licences are in `docs/fonts/`.
 *
 * Every size is in `sp`, so the whole scale moves with the system font-size setting.
 * That is deliberate and load-bearing: the layouts that hold this text avoid fixed
 * heights for the same reason. At 200% font scale a fixed-height ingredient row clips
 * its own text, and clipping the ingredient list of a poor student's dinner is not an
 * acceptable failure mode.
 */
val PlayfairDisplay = FontFamily(
    Font(R.font.playfair_display_regular, FontWeight.Normal),
    Font(R.font.playfair_display_semibold, FontWeight.SemiBold),
    Font(R.font.playfair_display_bold, FontWeight.Bold),
)

val DmSans = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_medium, FontWeight.Medium),
    Font(R.font.dm_sans_semibold, FontWeight.SemiBold),
)

val HakunaTypography = Typography(
    // Screen-level display: "Good morning, karibu chakula."
    displaySmall = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.25).sp,
    ),
    // Hero meal name on card
    headlineSmall = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    // Section headers, "This week's picks", "Best matches"
    titleLarge = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    // Taglines, ingredient rows, body copy
    bodyLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
    // Chip labels, nav labels, badges
    labelLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    // Eyebrow caps: "MENU BUILDER", "TODAY'S POCKET-FRIENDLY PICK"
    labelSmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.sp,
    ),
)

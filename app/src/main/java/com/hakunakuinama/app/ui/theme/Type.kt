package com.hakunakuinama.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hakunakuinama.app.R

/**
 * The app's type scale: two families, and no more.
 *
 * **Plus Jakarta Sans** carries anything that is a *name or a number* — screen titles,
 * section headings, meal names, the KES price, and the small-caps eyebrows. It is a
 * geometric sans with tall x-height and a tight, slightly squared-off feel, which is what
 * lets a food app read as editorial without a serif.
 *
 * **DM Sans** carries anything that has to be *read at 11–15sp* — taglines, ingredient
 * names, step text, tile labels, badges, the quantity column and the navigation. It is
 * more open at small sizes, so a 12sp footnote stays legible where a display-leaning face
 * starts to turn to mud.
 *
 * Two families rather than three is a deliberate constraint. The mockups mix a large
 * tight heading with a smaller, looser sans; the contrast here is weight and tracking
 * rather than "geometric versus humanist", so a screen never looks like two designs
 * stitched together. Sizes were measured back out of the mockups (the phone renders at
 * 0.62px per dp), so the proportions between levels match the design rather than being
 * a fresh guess.
 *
 * The fonts are **bundled**, not fetched. `ui-text-google-fonts` would resolve these
 * through Google Play Services, which means a silent fall back to the system default on
 * any device without Play Services, and a possible network fetch on first render. This app
 * promises that nothing leaves the device, so the TTFs ship in `res/font/` and the promise
 * holds on a de-Googled handset too.
 *
 * Static instances, not the variable originals. Upstream ships `PlusJakartaSans[wght]` and
 * `DMSans[opsz,wght]`; the files in `res/font/` were instanced to fixed weights (and
 * Jakarta subset to Latin) so there is no per-instance rasterisation cost while a list
 * scrolls. DM Sans is pinned to `opsz=14`, its text optical size — at 11–15sp the display
 * cut of that family is too loose. The OFL licences are in `docs/fonts/`.
 *
 * Every size is in `sp`, so the whole scale moves with the system font-size setting. That
 * is deliberate and load-bearing: the layouts holding this text avoid fixed heights for
 * the same reason. At 200% font scale a fixed-height ingredient row clips its own text,
 * and clipping the ingredient list of a poor student's dinner is not an acceptable failure
 * mode. Nothing here is set below 11sp for the same reason.
 */
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
)

val DmSans = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_medium, FontWeight.Medium),
    Font(R.font.dm_sans_semibold, FontWeight.SemiBold),
)

val HakunaTypography = Typography(
    // The big KES figure on the recipe screen.
    displaySmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.8).sp,
    ),
    // Screen titles: "Good morning, karibu chakula.", "What's in your kitchen?",
    // "Saved meals". Two of them stack on the Menu Builder, so the line height is set
    // tight enough that the pair still reads as one block.
    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 27.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.7).sp,
    ),
    // Section headings — "This week's picks", "Best matches", "What you'll need",
    // "How to cook" — and the recipe name.
    headlineSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 21.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.4).sp,
    ),
    // The hero card's meal name.
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.3).sp,
    ),
    // List-card meal names. Slightly tighter than the hero's: a list card's name has to
    // win against its tagline without shouting, and there is a price badge beside it.
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp,
    ),
    // Pantry tile labels and the ingredient table's quantity column.
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    // Ingredient names in the recipe table, and step instructions.
    bodyLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    // Card taglines.
    bodyMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
    // Footnotes, "Easy · 25 min", "cheapest first", the Swahili subtitle lines.
    bodySmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.sp,
    ),
    // Buttons, links, the pager label, the quantity column.
    labelLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    // KES badge text, nav labels, the tile position number.
    labelMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),
    // Eyebrow kickers: "MENU BUILDER", "TAP WHAT YOU HAVE", "YOUR KITCHEN SHELF". The caps
    // are applied at the call site with `.uppercase()` rather than stored in the string,
    // so a screen reader reads a word instead of spelling out letter-by-letter text that
    // is stored in capitals.
    labelSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 1.5.sp,
    ),
)

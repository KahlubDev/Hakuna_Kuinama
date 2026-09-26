package com.hakunakuinama.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Kenyan-inspired palette, modernised for Material 3.
 *
 * Three sources, in order of weight: the flag's black, red and green; a warm charcoal
 * that keeps "red" from feeling like an error colour; and an off-white paper tone, so a
 * recipe app does not read as a hospital chart.
 *
 * The M3 surface-container roles are deliberately not used. They only exist to describe
 * tonal elevation for M3's own components, and hard-coding them here would fight the
 * component defaults instead of complementing them.
 *
 * Every on-* colour was chosen for contrast against its own container, not for looks in
 * isolation — a red primary with dark text on it is how you get an unreadable button.
 */

// ---------------------------------------------------------------- light scheme

internal val HakunaRed = Color(0xFF8C1D18) // "nyama choma" brick red
private val RedContainer = Color(0xFFFFDAD6)
private val OnRedContainer = Color(0xFF3B0906)
private val RedLight = Color(0xFFFFB4AB)

internal val HakunaGreen = Color(0xFF2D6A2D) // flag green, desaturated for a tertiary role
private val GreenContainer = Color(0xFFC4F2BD)
private val OnGreenContainer = Color(0xFF052100)
private val GreenLight = Color(0xFFA8D6A2)
private val GreenContainerDark = Color(0xFF1F4F1E)

private val WarmPaper = Color(0xFFFFFBF8)
private val Charcoal = Color(0xFF121214)
private val Ink = Color(0xFF201A19)
private val InkLight = Color(0xFFEDE0DE)
private val Terracotta = Color(0xFF775652)
private val TerracottaContainer = Color(0xFFF5DDD8)
private val OnTerracottaContainer = Color(0xFF2C1512)
private val TerracottaLight = Color(0xFFE7BDB7)
private val TerracottaContainerDark = Color(0xFF5D3F3C)
private val Blush = Color(0xFFF5DDDA)
private val OnBlush = Color(0xFF534341)
private val Stone = Color(0xFF857371)
private val StoneLight = Color(0xFFA08C8A)

val LightColors = lightColorScheme(
    primary = HakunaRed,
    onPrimary = Color.White,
    primaryContainer = RedContainer,
    onPrimaryContainer = OnRedContainer,
    secondary = Terracotta,
    onSecondary = Color.White,
    secondaryContainer = TerracottaContainer,
    onSecondaryContainer = OnTerracottaContainer,
    tertiary = HakunaGreen,
    onTertiary = Color.White,
    tertiaryContainer = GreenContainer,
    onTertiaryContainer = OnGreenContainer,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = WarmPaper,
    onBackground = Ink,
    surface = WarmPaper,
    onSurface = Ink,
    surfaceVariant = Blush,
    onSurfaceVariant = OnBlush,
    outline = Stone,
    outlineVariant = Color(0xFFD8C2C0),
    scrim = Color.Black,
    inverseSurface = Color(0xFF362F2E),
    inverseOnSurface = Color(0xFFFBEEEC),
    inversePrimary = RedLight,
    surfaceTint = HakunaRed,
)

// ---------------------------------------------------------------- dark scheme

val DarkColors = darkColorScheme(
    primary = RedLight,
    onPrimary = Color(0xFF690005),
    primaryContainer = HakunaRed,
    onPrimaryContainer = RedContainer,
    secondary = TerracottaLight,
    onSecondary = Color(0xFF442926),
    secondaryContainer = TerracottaContainerDark,
    onSecondaryContainer = TerracottaContainer,
    tertiary = GreenLight,
    onTertiary = Color(0xFF11380F),
    tertiaryContainer = GreenContainerDark,
    onTertiaryContainer = GreenContainer,
    error = RedLight,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Charcoal,
    onBackground = InkLight,
    surface = Charcoal,
    onSurface = InkLight,
    surfaceVariant = Color(0xFF534341),
    onSurfaceVariant = Color(0xFFD8C2C0),
    outline = StoneLight,
    outlineVariant = Color(0xFF534341),
    scrim = Color.Black,
    inverseSurface = InkLight,
    inverseOnSurface = Color(0xFF362F2E),
    inversePrimary = HakunaRed,
    surfaceTint = RedLight,
)

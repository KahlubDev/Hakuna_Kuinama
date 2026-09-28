package com.hakunakuinama.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ── Palette ──────────────────────────────────────────────────────────────────

// Warm paper backgrounds
val WarmPaper = Color(0xFFF5F0EB)
val WarmPaperDark = Color(0xFF1B1713)

// Teal / forest green — primary accent (chips, nav, badges)
val TealForest = Color(0xFF3D6B5E)
val TealForestLight = Color(0xFF5A9080)
val TealContainer = Color(0xFFD4EAE3)
val OnTealContainer = Color(0xFF0D2A22)

// Terracotta — KES price badges, secondary accent
val Terra = Color(0xFF5C3B2E)
val TerraLight = Color(0xFF8C6254)
val TerraContainer = Color(0xFFF2DDD5)
val OnTerraContainer = Color(0xFF23100A)

// Artwork colors (RecipeImage placeholder blocks)
val ArtworkTeal = Color(0xFF4A7A6A)
val ArtworkBrown = Color(0xFF7A5C42)
val ArtworkStone = Color(0xFFE8DDD5)

// Text
val Ink = Color(0xFF1A1410)
val InkMuted = Color(0xFF7A6E68)
val InkLight = Color(0xFFEDE5DC)

// Surfaces
val SurfaceLight = Color(0xFFFAF8F5)
val SurfaceDark = Color(0xFF252018)
val CardSurfaceDark = Color(0xFF2C2520)

// Dividers / outlines
val Divider = Color(0xFFE8DDD5)
val DividerDark = Color(0xFF3A332C)

// ── Schemes ──────────────────────────────────────────────────────────────────

val LightColors = lightColorScheme(
    primary = TealForest,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = OnTealContainer,
    secondary = Terra,
    onSecondary = Color.White,
    secondaryContainer = TerraContainer,
    onSecondaryContainer = OnTerraContainer,
    tertiary = ArtworkBrown,
    onTertiary = Color.White,
    tertiaryContainer = ArtworkStone,
    onTertiaryContainer = Ink,
    background = WarmPaper,
    onBackground = Ink,
    surface = SurfaceLight,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEDE5DC),
    onSurfaceVariant = InkMuted,
    outline = Color(0xFFC8B8A8),
    outlineVariant = Divider,
    scrim = Color.Black,
    inverseSurface = Color(0xFF302820),
    inverseOnSurface = InkLight,
    inversePrimary = TealForestLight,
    surfaceTint = TealForest,
)

val DarkColors = darkColorScheme(
    primary = TealForestLight,
    onPrimary = Color(0xFF0D2A22),
    primaryContainer = TealForest,
    onPrimaryContainer = TealContainer,
    secondary = TerraLight,
    onSecondary = Color(0xFF23100A),
    secondaryContainer = Color(0xFF3D2C24),
    onSecondaryContainer = TerraContainer,
    tertiary = Color(0xFFBCA898),
    onTertiary = Color(0xFF2C1A10),
    tertiaryContainer = Color(0xFF3A2A20),
    onTertiaryContainer = InkLight,
    background = WarmPaperDark,
    onBackground = InkLight,
    surface = SurfaceDark,
    onSurface = InkLight,
    surfaceVariant = Color(0xFF3A332C),
    onSurfaceVariant = Color(0xFFC8B8A8),
    outline = Color(0xFF5E5248),
    outlineVariant = DividerDark,
    scrim = Color.Black,
    inverseSurface = InkLight,
    inverseOnSurface = Color(0xFF302820),
    inversePrimary = TealForest,
    surfaceTint = TealForestLight,
)

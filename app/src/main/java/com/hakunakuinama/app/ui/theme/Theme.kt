package com.hakunakuinama.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * The app's single theme entry point. Every screen renders inside this, so no
 * composable ever has to know whether dark mode is on.
 *
 * Dynamic colour is **off by default** and the flag is not exposed to callers. Wallpaper
 * tinting would replace the palette the brand is built on — a food app whose primary
 * colour is whatever the user's home screen happens to be. It stays available as a
 * parameter only so a future "try my wallpaper colours" experiment is one line away.
 */
@Composable
fun HakunaKuinamaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HakunaTypography,
        shapes = HakunaShapes,
        content = content,
    )
}

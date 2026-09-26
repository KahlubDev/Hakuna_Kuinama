package com.hakunakuinama.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hakunakuinama.app.ui.navigation.HakunaApp
import com.hakunakuinama.app.ui.theme.HakunaKuinamaTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The app's only Activity. It installs the splash screen, turns on edge-to-edge, and
 * hands straight to [HakunaApp] — everything else lives in the composable tree, which is
 * what keeps previews and screenshot tests possible.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate, or the system splash is already gone.
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            HakunaKuinamaTheme {
                HakunaApp()
            }
        }
    }
}

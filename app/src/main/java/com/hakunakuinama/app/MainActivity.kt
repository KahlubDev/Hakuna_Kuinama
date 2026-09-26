package com.hakunakuinama.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host. The real Compose tree (theme, NavHost, screens) is Phase 3 —
 * right now this only proves the Hilt + Room + Compose skeleton builds and launches.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            Surface {
                Text("Hakuna Kuinama — Phase 1 data layer is in place.")
            }
        }
    }
}

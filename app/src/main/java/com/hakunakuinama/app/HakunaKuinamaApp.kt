package com.hakunakuinama.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point + Hilt root.
 *
 * Hilt modules (DatabaseModule, RepositoryModule) are intentionally NOT wired here yet —
 * that is Phase 2. Until then the app compiles but the object graph is incomplete.
 */
@HiltAndroidApp
class HakunaKuinamaApp : Application()

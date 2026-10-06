plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)   // required by Kotlin 2.x for Compose compilation
    alias(libs.plugins.ksp)               // Room + Hilt annotation processing
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.hakunakuinama.app"
    // Play has required targetSdk 36 (Android 16) for new apps and updates since
    // 31 August 2026. compileSdk is held to the same number deliberately: compiling against
    // an older platform than you target hides exactly the API 36 behaviour changes that
    // targeting them is meant to surface.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hakunakuinama.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // Room reads the *previous* schema JSON to know what it is migrating from, and
    // MigrationTestHelper looks for it in the androidTest APK's assets under
    // "<database canonical name>/<version>.json". The KSP arg above exports schemas into
    // app/schemas/<canonical name>/, so adding that directory as an androidTest asset root
    // publishes them at exactly the path Room looks for. Without this, every migration test
    // fails with Room's generic "Cannot find the schema file in the assets folder".
    sourceSets {
        getByName("androidTest") {
            assets.srcDir("$projectDir/schemas")
        }
    }

    packaging {
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/LICENSE.md", "META-INF/LICENSE-notice.md")
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Export Room schemas so migrations can be diff-tested in Phase 4.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
}

dependencies {
    // ---------- AndroidX foundation ----------
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.splashscreen)
    implementation(libs.kotlinx.coroutines.android)

    // ---------- Compose + Material 3 (versions from BOM) ----------
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // ---------- Lifecycle / Navigation ----------
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // ---------- Dependency injection (Hilt) ----------
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // ---------- Persistence (Room) ----------
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ---------- Settings store ----------
    implementation(libs.androidx.datastore.preferences)

    // ---------- Images ----------
    implementation(libs.coil.compose)

    // ---------- Unit tests ----------
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)

    // ---------- Instrumented tests ----------
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    // MigrationTestHelper, used by MigrationTestHelperTest. This was on testImplementation,
    // where nothing in src/test needs it and androidTest cannot see it — which is why the
    // instrumentation suite had never compiled.
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

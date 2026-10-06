package com.hakunakuinama.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration tests. **These require a connected device or emulator** — they need real
 * SQLite, so they cannot run on the JVM:
 *
 * ```
 * ./gradlew :app:connectedDebugAndroidTest
 * ```
 *
 * There is nothing to assert at version 1, because there is no migration yet. This file
 * exists so adding version 2 is a copy-paste job rather than a research project, and so
 * the *shape* of the test is reviewable before it matters.
 *
 * Read [HakunaKuinamaMigrations] first: it explains the policy this test enforces.
 *
 * ### How the schema reaches the device
 *
 * Room needs the version 1 schema to know what it is migrating *from*, and
 * `MigrationTestHelper` reads it from the **androidTest APK's assets**, at
 * `<database canonical name>/<version>.json`. That is not where the build puts it: the KSP
 * `room.schemaLocation` arg exports it into
 * `app/schemas/com.hakunakuinama.app.data.local.HakunaKuinamaDatabase/1.json`, and it has
 * to be on disk in version control as well as on the device.
 *
 * The `androidTest` assets `srcDir` in `build.gradle.kts` is what bridges the two. If you
 * ever see "Cannot find the schema file in the assets folder", that block is the first
 * thing to check — not the schema file itself, which is committed and correct.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTestHelperTest {

    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        HakunaKuinamaDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    /**
     * The version 1 schema must be committed *and* published into the test APK's assets
     * before a 1 -> 2 migration can be written or tested at all. Without it Room has nothing
     * to migrate from, CI cannot verify a migration, and the only way to find out is on a
     * user's phone.
     *
     * This asserts on the **asset**, not on a path in the repository. An earlier version
     * checked `File("app/schemas/1.json")`, which could never have worked: this code runs
     * on the device, where the repository checkout does not exist and the working directory
     * is `/`. It also had the wrong filename — Room nests the JSON under the database's
     * canonical name.
     */
    @Test
    fun version1SchemaIsPublishedToTheTestAssets() {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val assetPath = "$SCHEMA_ASSET_FOLDER/$VERSION_1.json"

        val schemaJson = try {
            assets.open(assetPath).bufferedReader().use { it.readText() }
        } catch (cause: java.io.IOException) {
            throw AssertionError(
                "Schema $assetPath is not in the androidTest APK's assets, so no migration " +
                    "test can run. Check two things: that $SCHEMA_ASSET_FOLDER/$VERSION_1.json " +
                    "is committed to the repository, and that build.gradle.kts adds " +
                    "\$projectDir/schemas as an androidTest assets srcDir.",
                cause,
            )
        }

        // Room's exported bundle is JSON; an empty or truncated file parses as nothing useful
        // and would surface as a confusing schema error deep inside createDatabase.
        assertTrue(
            "Schema $assetPath was found but is empty, so it was exported badly.",
            schemaJson.isNotBlank(),
        )
        assertTrue(
            "Schema $assetPath does not look like Room's exported schema bundle " +
                "(expected a JSON object with a \"formatVersion\").",
            schemaJson.contains("formatVersion"),
        )
    }

    /**
     * Proves Room can build and validate a version 1 database from the committed schema,
     * which is the precondition for every migration test that follows. If the schema asset is
     * missing or malformed, `createDatabase` throws.
     */
    @Test
    fun migrationHarnessIsWiredUp() {
        // The lambda parameter type is spelled out on purpose. `SupportSQLiteDatabase` is both
        // a Closeable and an AutoCloseable, so `use` resolves against both extensions and the
        // lambda parameter type cannot be inferred; Kotlin reports it as "Cannot infer type for
        // this parameter". Do not "simplify" this back to `use { }`.
        helper.createDatabase(TEST_DB, 1).use { db: SupportSQLiteDatabase ->
            assertEquals(1, db.version)
        }
    }

    /**
     * Template for the real 1 -> 2 case. Once a migration exists, this becomes:
     *
     * ```kotlin
     * @Test
     * fun migratesFrom1To2() = helper.runMigrationsAndValidate(TEST_DB, 2, true).use { db ->
     *     // query the new schema here, e.g. it queries the new column
     *     assertTrue(db.query("SELECT imageUrl FROM meals LIMIT 1").use { it.moveToFirst() })
     * }
     * ```
     *
     * `validateDroppedTables = true` is the argument people get wrong: it makes Room fail
     * the test if the migration leaves a table behind that the new schema no longer
     * declares, which is precisely the kind of silent data problem that is invisible until
     * a user's list of saved recipes stops showing up.
     */
    private companion object {
        const val TEST_DB = "migration-test.db"
        const val VERSION_1 = 1

        /** Must match [HakunaKuinamaDatabase]'s canonical name — Room keys the asset off it. */
        const val SCHEMA_ASSET_FOLDER = "com.hakunakuinama.app.data.local.HakunaKuinamaDatabase"
    }
}

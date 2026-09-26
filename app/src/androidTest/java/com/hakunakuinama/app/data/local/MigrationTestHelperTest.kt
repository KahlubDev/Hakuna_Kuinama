package com.hakunakuinama.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
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
 * Note this file is the one thing in the project I could not compile — `androidTest` needs
 * the AndroidX test artifacts and a device. The JVM tests in `src/test` are all verified.
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
     * The version 1 schema must be exported and committed before a 1 -> 2 migration can be
     * written or tested at all: Room reads the previous schema JSON to know what it is
     * migrating *from*. Without `app/schemas/1.json` in version control, CI cannot verify
     * a migration, and the only way to find out is on a user's phone.
     */
    @Test
    fun version1SchemaIsExportedAndCommitted() {
        val schemas = InstrumentationRegistry.getInstrumentation().context
            .filesDir
            .resolve("schemas")
        val exportedInRepo = listOf(
            File("app/schemas/1.json"),
            File("../app/schemas/1.json"),
        ).any { it.exists() }

        assertTrue(
            "app/schemas/1.json must exist in the repository for migration tests to work. " +
                "Searched near: ${schemas.absolutePath}",
            exportedInRepo,
        )
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
    @Test
    fun migrationHarnessIsWiredUp() {
        // Creates a fresh version-1 database from the committed schema and closes it. Proves
        // the helper can read app/schemas/1.json, which is the precondition for every
        // migration test that follows.
        helper.createDatabase(TEST_DB, 1).use { /* created and validated, then closed */ }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}

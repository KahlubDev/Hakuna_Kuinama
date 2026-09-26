package com.hakunakuinama.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migration policy for Hakuna Kuinama.
 *
 * **The database is version 1, so there is no migration to write yet.** This file is the
 * mechanism and the contract for when there is, because the failure mode of getting it
 * wrong is severe and invisible until it ships.
 *
 * ## Why there is deliberately no destructive fallback
 *
 * `Room.databaseBuilder(...).fallbackToDestructiveMigration()` is the single most
 * dangerous line you can add to an app that stores user data. It means: a version bump
 * without a migration silently deletes the database. For this app that is a user's
 * saved recipes and their weekly shopping list, gone with no warning and no undo. So the
 * builder sets **no** fallback at all, and Room's default behaviour stands: an unmigrated
 * schema change throws on first query.
 *
 * That default is scary, and that is the point. A crash on the developer's machine the
 * day they bump the version is a nuisance; the same bug discovered by users in the wild is
 * their data. We take the loud failure.
 *
 * ## How to ship version 2
 *
 * 1. Bump `version` in [HakunaKuinamaDatabase] to 2.
 * 2. If the change is purely additive (a new table, a new column with a default, a new
 *    index), add an `AutoMigration` spec instead of hand-writing SQL:
 *    ```kotlin
 *    class Migration1To2 : AutoMigration(from = 1, to = 2) {
 *        class Spec : AutoMigrationSpec()
 *    }
 *    ```
 *    then add it to [ALL]. Room generates and, more importantly, *verifies* the SQL
 *    against the exported schemas at build time.
 * 3. If the change rewrites or drops data, hand-write it:
 *    ```kotlin
 *    val MIGRATION_1_2 = object : Migration(1, 2) {
 *        override fun migrate(db: SupportSQLiteDatabase) {
 *            db.execSQL("ALTER TABLE meals ADD COLUMN imageUrl TEXT")
 *        }
 *    }
 *    ```
 *    and add it to [ALL].
 * 4. Add a case to `MigrationTestHelperTest` in `src/androidTest`. That test is the only
 *    thing standing between a hand-written migration and a corrupt production database, so
 *    do not skip it — Room's exported schema JSON is what makes the test possible, which
 *    is why `app/schemas/` is committed.
 * 5. Run `./gradlew :app:connectedAndroidTest` on a device or emulator. Migration tests
 *    cannot run on the JVM: they need real SQLite.
 */
object HakunaKuinamaMigrations {

    /**
     * Every migration, applied in order. Empty at version 1; the first entry arrives with
     * version 2.
     */
    val ALL: Array<Migration> = emptyArray()
}

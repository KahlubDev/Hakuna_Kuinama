package com.hakunakuinama.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hakunakuinama.app.data.local.dao.GroceryDao
import com.hakunakuinama.app.data.local.dao.IngredientDao
import com.hakunakuinama.app.data.local.dao.MealDao
import com.hakunakuinama.app.data.local.entity.GroceryItemEntity
import com.hakunakuinama.app.data.local.entity.IngredientEntity
import com.hakunakuinama.app.data.local.entity.MealEntity
import com.hakunakuinama.app.data.local.entity.MealIngredientCrossRef
import com.hakunakuinama.app.data.local.entity.MealStepEntity
import com.hakunakuinama.app.data.local.seed.SeedDatabaseCallback
import kotlinx.coroutines.CoroutineScope

@Database(
    entities = [
        MealEntity::class,
        IngredientEntity::class,
        MealStepEntity::class,
        MealIngredientCrossRef::class,
        GroceryItemEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class HakunaKuinamaDatabase : RoomDatabase() {

    abstract fun mealDao(): MealDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun groceryDao(): GroceryDao

    companion object {
        const val DB_NAME = "hakuna_kuinama.db"

        /**
         * Wiring notes:
         *
         * * Foreign keys are ON by default in Room, which is what we want: a recipe owns its
         *   steps and ingredient usages, so cascade delete keeps orphans out.
         * * The recipe seed is triggered from [SeedDatabaseCallback] on a *separate*
         *   application coroutine, never from inside `onCreate` itself — writing to the DB
         *   from that callback while Room still holds the creation transaction deadlocks.
         * * The seeder is count-guarded (`seedIfEmpty`), so it can never clobber a user's
         *   favourites or shopping lists.
         * * Known pre-release limitation: a count guard means recipes added in a *later*
         *   app version never reach existing installs. Before v1.0, either ship a prepackaged
         *   DB (`createFromAsset`) or add a `seed_version` row and re-seed when it is behind.
         *
         * @param applicationScope A scope that outlives any single screen (Phase 2 wires this
         *   with `@ApplicationScope`).
         * @param seeder Resolves the database lazily, breaking the Room ↔ seeder init cycle.
         */
        fun build(
            context: Context,
            applicationScope: CoroutineScope,
            seeder: SeedDatabaseCallback,
        ): HakunaKuinamaDatabase =
            Room.databaseBuilder(context, HakunaKuinamaDatabase::class.java, DB_NAME)
                // No fallbackToDestructiveMigration, ever. See HakunaKuinamaMigrations for
                // why an unmigrated schema change must fail loudly rather than quietly
                // delete a user's saved recipes.
                .addMigrations(*HakunaKuinamaMigrations.ALL)
                .addCallback(seeder)
                .build()
    }
}

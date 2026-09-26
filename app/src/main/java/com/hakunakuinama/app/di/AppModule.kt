package com.hakunakuinama.app.di

import android.content.Context
import com.hakunakuinama.app.data.local.HakunaKuinamaDatabase
import com.hakunakuinama.app.data.local.dao.GroceryDao
import com.hakunakuinama.app.data.local.dao.IngredientDao
import com.hakunakuinama.app.data.local.dao.MealDao
import com.hakunakuinama.app.data.local.seed.DatabaseSeeder
import com.hakunakuinama.app.data.local.seed.SeedDatabaseCallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * A [CoroutineScope] that lives as long as the process.
 *
 * Qualified because unqualified `CoroutineScope` and `Dispatcher` bindings are a classic
 * Hilt footgun: any component could inject a scope tied to the wrong lifetime and leak a
 * Room callback into a screen's job.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/** Application-wide singletons: the Room database, the seeding callback, and the clock. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * [SupervisorJob] so one failed child (a seed that throws) does not cancel the scope
     * and silently stop every future database callback.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * The wall clock used for "what time is it" logic — meal slots and weekly budgets.
     *
     * Deliberately **not** `android.os.SystemClock`: that is a monotonic uptime clock for
     * measuring durations (it does not know the date, so `LocalDate.now()` built from it
     * is meaningless). Time-of-day features need the real civil time in the device's zone.
     * Injected rather than called statically so tests can pin "now" to a Tuesday evening.
     */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    /**
     * Room database, created lazily — `databaseBuilder(...).build()` only opens the file
     * on first query, so this is safe on the main thread.
     *
     * [seeder] takes a `Provider` of this very database (see `DatabaseSeeder`), so handing
     * it in here does not create a dependency cycle: the reference is lazy, and the
     * seeder is only invoked from Room's onCreate callback, long after this returns.
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        @ApplicationScope applicationScope: CoroutineScope,
        seeder: DatabaseSeeder,
    ): HakunaKuinamaDatabase = HakunaKuinamaDatabase.build(
        context = context,
        applicationScope = applicationScope,
        seeder = SeedDatabaseCallback(applicationScope, seeder),
    )

    /**
     * The DAOs are pulled off the database instance rather than constructed by Hilt —
     * Room owns their implementation, so Dagger has to be told where they come from.
     * Without these three, the graph fails to compile with "MealDao cannot be provided".
     *
     * Scoped because `getMealDao()` hands back a fresh wrapper on every call, and the
     * wrapper is cheap but not free; the repository only ever needs one.
     */
    @Provides
    @Singleton
    fun provideMealDao(database: HakunaKuinamaDatabase): MealDao = database.mealDao()

    @Provides
    @Singleton
    fun provideIngredientDao(database: HakunaKuinamaDatabase): IngredientDao = database.ingredientDao()

    @Provides
    @Singleton
    fun provideGroceryDao(database: HakunaKuinamaDatabase): GroceryDao = database.groceryDao()
}

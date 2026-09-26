package com.hakunakuinama.app.di

import com.hakunakuinama.app.data.repository.MealRepositoryImpl
import com.hakunakuinama.app.domain.repository.MealRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Interface → implementation bindings.
 *
 * Split from [AppModule] on purpose: bindings are compile-time checked by Hilt's
 * generated code (a rename here fails the build), while `AppModule` provides values that
 * only fail at runtime. Keeping them apart makes the risky file small.
 *
 * ViewModels get the interface injected, never [MealRepositoryImpl] — that is what lets
 * Phase 4's ViewModel tests swap in a fake without any Hilt involvement.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMealRepository(implementation: MealRepositoryImpl): MealRepository
}

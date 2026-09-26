package com.hakunakuinama.app.domain.usecase

import com.hakunakuinama.app.domain.model.Meal
import com.hakunakuinama.app.domain.repository.MealRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * The user's saved meals, most recently saved first.
 *
 * Thin by design — the ordering and the "is it still a favourite" filtering both happen
 * in SQL, where they are index-backed. This exists as the single named entry point the
 * Favorites screen (and Phase 4's tests) depend on, so a future "group favourites by
 * slot" rule has one place to live.
 */
class GetFavoriteMealsUseCase @Inject constructor(
    private val mealRepository: MealRepository,
) {
    operator fun invoke(): Flow<List<Meal>> = mealRepository.observeFavourites()
}

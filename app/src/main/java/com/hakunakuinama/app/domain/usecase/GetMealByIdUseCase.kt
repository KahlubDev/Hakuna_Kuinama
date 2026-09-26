package com.hakunakuinama.app.domain.usecase

import com.hakunakuinama.app.domain.model.Meal
import com.hakunakuinama.app.domain.repository.MealRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Streams one full recipe — steps, ingredient usages and the derived KES cost.
 *
 * Returns a [Flow] rather than a value because the recipe screen should also react to the
 * user favouriting it (the FAB reflects the flag) without a manual refresh. A `null`
 * emission means "not found", which the UI turns into an empty state — not an error, since
 * a deleted id is a normal navigation outcome.
 */
class GetMealByIdUseCase @Inject constructor(
    private val mealRepository: MealRepository,
) {
    operator fun invoke(mealId: Long): Flow<Meal?> = mealRepository.observeMeal(mealId)
}

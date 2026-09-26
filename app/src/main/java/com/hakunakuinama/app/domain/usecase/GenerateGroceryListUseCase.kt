package com.hakunakuinama.app.domain.usecase

import com.hakunakuinama.app.domain.model.GroceryPlan
import com.hakunakuinama.app.domain.repository.MealRepository
import javax.inject.Inject

/**
 * Turns selected recipes into a shopping list.
 *
 * Returns [Result] rather than throwing: a failed list generation is something to show
 * the user and recover from, not a reason to crash a screen they were reading a recipe
 * on. The repository reports how many lines were written on success.
 */
class GenerateGroceryListUseCase @Inject constructor(
    private val mealRepository: MealRepository,
) {
    suspend operator fun invoke(plan: GroceryPlan): Result<Int> =
        mealRepository.generateGroceryList(plan)
}

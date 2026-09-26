package com.hakunakuinama.app.domain.usecase

import com.hakunakuinama.app.domain.model.Ingredient
import com.hakunakuinama.app.domain.repository.MealRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * The ingredient catalogue that backs the Menu Builder's chip grid.
 *
 * Added because the Menu Builder needs two separate things — the chips it renders and the
 * ranking of the ticked ones — and ViewModels are not allowed to touch the repository
 * directly. Ordering (staples first, then alphabetical) comes from the DAO query.
 */
class GetIngredientsUseCase @Inject constructor(
    private val mealRepository: MealRepository,
) {
    operator fun invoke(): Flow<List<Ingredient>> = mealRepository.observeIngredients()
}

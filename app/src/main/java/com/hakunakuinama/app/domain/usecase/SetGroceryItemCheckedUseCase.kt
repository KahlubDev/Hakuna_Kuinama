package com.hakunakuinama.app.domain.usecase

import com.hakunakuinama.app.domain.repository.MealRepository
import javax.inject.Inject

/**
 * Ticks a line off the shopping list.
 *
 * Idempotent by design: the caller passes the state it wants, so a double tap cannot
 * leave the checkbox and the database disagreeing about whether something was bought.
 */
class SetGroceryItemCheckedUseCase @Inject constructor(
    private val mealRepository: MealRepository,
) {
    suspend operator fun invoke(itemId: Long, isChecked: Boolean) {
        mealRepository.setGroceryItemChecked(itemId, isChecked)
    }
}

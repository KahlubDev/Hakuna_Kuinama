package com.hakunakuinama.app.domain.usecase

import com.hakunakuinama.app.domain.model.GroceryItem
import com.hakunakuinama.app.domain.repository.MealRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** The merged shopping list for one week. [weekStart] is snapped to that week's Monday. */
class GetWeeklyGroceryListUseCase @Inject constructor(
    private val mealRepository: MealRepository,
) {
    operator fun invoke(weekStart: LocalDate): Flow<List<GroceryItem>> =
        mealRepository.observeGroceryList(weekStart)
}

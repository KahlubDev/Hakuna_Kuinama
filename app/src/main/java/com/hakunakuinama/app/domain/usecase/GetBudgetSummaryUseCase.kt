package com.hakunakuinama.app.domain.usecase

import com.hakunakuinama.app.domain.model.BudgetSummary
import com.hakunakuinama.app.domain.repository.MealRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Planned / spent / outstanding money for one week, in KES. */
class GetBudgetSummaryUseCase @Inject constructor(
    private val mealRepository: MealRepository,
) {
    operator fun invoke(weekStart: LocalDate): Flow<BudgetSummary> =
        mealRepository.observeBudgetSummary(weekStart)
}

package com.hakunakuinama.app.domain.usecase

import com.hakunakuinama.app.domain.model.Meal
import com.hakunakuinama.app.domain.repository.MealRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The Home screen's "This week's picks": the whole catalogue, cheapest plate first.
 *
 * The ordering is the point. The app's promise is that it answers "what will the week cost
 * me?" for someone on a tight budget, so a list ordered by cuisine or by how good the
 * photography is would answer a question nobody asked. Cheapest plate first means the first
 * card is one they can definitely afford, and the list stays affordable all the way down.
 *
 * The suggested meal is *not* filtered out here. This use case has no idea which meal the
 * dashboard is featuring — that is resolved from the clock in [GetSuggestedMealUseCase] and
 * would be a second source of truth about "today's pick" if it were duplicated here. The
 * screen drops the duplicate instead, and falls back to the unfiltered list if the featured
 * meal is the only one there is.
 */
class GetWeeklyPicksUseCase @Inject constructor(
    private val mealRepository: MealRepository,
) {
    operator fun invoke(): Flow<List<Meal>> = mealRepository.observeMeals()
        .map { meals ->
            meals.sortedWith(
                compareBy<Meal> { it.costPerServingKes }
                    // Name as the tie-break, so two equally-priced plates always appear in
                    // the same order and the list does not reshuffle between emissions.
                    .thenBy { it.name },
            )
        }
}

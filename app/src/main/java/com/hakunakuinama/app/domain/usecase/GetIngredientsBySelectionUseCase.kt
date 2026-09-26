package com.hakunakuinama.app.domain.usecase

import com.hakunakuinama.app.domain.model.MealMatch
import com.hakunakuinama.app.domain.repository.MealRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * "I have these ingredients" → ranked recipes.
 *
 * The scoring itself is **not** re-implemented here. It lives in [MealMatch.of] (verified
 * in Phase 1) and is applied by the repository to every recipe; duplicating the rules
 * would let the Menu Builder and the "missing cost" badge drift apart within a release.
 * What this use case owns is the input contract the UI wants: a plain list of ticked ids.
 *
 * Results are ordered by the repository: "cook now" first, then match percentage, then
 * cheapest plate — a partial match the student can afford beats a better match they
 * cannot.
 */
class GetIngredientsBySelectionUseCase @Inject constructor(
    private val mealRepository: MealRepository,
) {

    /** One-shot read, for tests and one-shot callers. */
    suspend operator fun invoke(selectedIngredientIds: List<Long>): List<MealMatch> =
        observe(flowOf(selectedIngredientIds)).first()

    /**
     * Reactive read for the Menu Builder: pass the user's selection as a [Flow] and get
     * re-ranked results as it changes, without re-querying the database per tap.
     *
     * [distinctUntilChanged] matters here — tapping chips in a different order produces an
     * equal set, and without it every tap would re-run the scoring for nothing.
     */
    fun observe(selectedIngredientIds: Flow<List<Long>>): Flow<List<MealMatch>> =
        mealRepository.observeMatches(
            selectedIngredientIds
                .map { it.toSet() }
                .distinctUntilChanged(),
        )
}

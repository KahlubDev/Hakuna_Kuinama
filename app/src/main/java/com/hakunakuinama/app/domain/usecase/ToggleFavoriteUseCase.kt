package com.hakunakuinama.app.domain.usecase

import com.hakunakuinama.app.domain.repository.MealRepository
import com.hakunakuinama.app.domain.util.resultOf
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Flips a meal's favourite flag and reports the state it landed on.
 *
 * @return the new favourite state on success, so the caller can show the right snackbar
 *         ("Saved to favorites" / "Removed from favorites") without reading the DB again.
 *         Failures come back as a failed [Result] instead of an exception, because a
 *         failed write is a UI message, not a crash.
 */
class ToggleFavoriteUseCase @Inject constructor(
    private val mealRepository: MealRepository,
) {

    suspend operator fun invoke(mealId: Long): Result<Boolean> = resultOf {
        // Read-then-write. The alternative is an atomic "UPDATE ... SET isFavourite =
        // NOT isFavourite" in SQL, which cannot also set favouritedAt without a second
        // statement. The window is a single frame wide, and the repository's flow pushes
        // the authoritative value back to the UI, so a double tap self-corrects.
        val isFavourite = mealRepository.observeIsFavourite(mealId).first()
        val next = !isFavourite
        mealRepository.setFavourite(mealId, next)
        next
    }
}

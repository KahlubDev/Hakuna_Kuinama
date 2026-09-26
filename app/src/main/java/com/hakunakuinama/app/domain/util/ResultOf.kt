package com.hakunakuinama.app.domain.util

import kotlinx.coroutines.CancellationException

/**
 * Runs [block] and wraps the outcome in a [Result].
 *
 * This exists instead of `runCatching` because `runCatching` also catches
 * [CancellationException]. That quietly breaks coroutine cancellation: a user who closes
 * the recipe screen mid-save would leave the write running, and any `Result` produced
 * afterwards would report a failure that nobody is waiting for any more. Here,
 * cancellation is always rethrown and only genuine errors become a failed [Result].
 */
suspend inline fun <T> resultOf(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (error: Throwable) {
    Result.failure(error)
}

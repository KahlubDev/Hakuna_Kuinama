package com.hakunakuinama.app.data.local

import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs a block inside a single Room transaction.
 *
 * The repository depends on this instead of on the DAOs alone so operations that are
 * several DAO calls (clear the week, re-insert the merged list) cannot be observed
 * half-applied, and cannot be lost to a crash halfway through. Unit tests swap in a fake
 * that just runs the block.
 */
@Singleton
class TransactionRunner @Inject constructor(
    private val database: HakunaKuinamaDatabase,
) {
    suspend fun <R> run(block: suspend () -> R): R = database.withTransaction { block() }
}

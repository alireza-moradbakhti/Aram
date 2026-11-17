package com.aram.core.domain.repository

import com.aram.core.model.Outcome
import com.aram.core.model.Transaction
import com.aram.core.domain.transaction.TransactionDraft
import kotlinx.coroutines.flow.Flow

/**
 * The only way presentation touches the ledger. Implementations own ids,
 * timestamps, and soft-delete tombstones; drafts arrive already validated.
 */
interface TransactionRepository {

    fun observeTransactions(
        fromInclusive: Long = Long.MIN_VALUE,
        toExclusive: Long = Long.MAX_VALUE,
    ): Flow<List<Transaction>>

    fun observeRecent(limit: Int): Flow<List<Transaction>>

    fun observeTransaction(id: String): Flow<Transaction?>

    suspend fun create(draft: TransactionDraft): Outcome<Transaction>

    suspend fun update(id: String, draft: TransactionDraft): Outcome<Transaction>

    /** Marks the row as deleted; it stays as a tombstone for sync. */
    suspend fun softDelete(id: String): Outcome<Unit>
}

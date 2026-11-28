package com.aram.core.domain.repository

import com.aram.core.model.Outcome
import com.aram.core.model.Transaction
import com.aram.core.domain.transaction.TransactionDraft
import com.aram.core.domain.transaction.TransferDraft
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

    /**
     * Writes the signed `TRANSFER` pair in one local transaction and returns
     * the shared group id. The pair stays on the device: the API models only
     * income and expense, so no push is queued.
     */
    suspend fun createTransfer(draft: TransferDraft): Outcome<String>

    suspend fun update(id: String, draft: TransactionDraft): Outcome<Transaction>

    /**
     * Marks the row as deleted; it stays as a tombstone for sync. Deleting a
     * transfer leg tombstones every leg of the group, since a transfer only
     * makes sense as a pair.
     */
    suspend fun softDelete(id: String): Outcome<Unit>
}

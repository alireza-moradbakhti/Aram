package com.aram.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.aram.core.database.entity.SyncOutboxEntity
import com.aram.core.database.entity.TransactionEntity

/**
 * Atomic ledger write plus outbox enqueue. Keeping both rows in one Room
 * transaction means a process death can never leave a local write that nobody
 * will push, or an outbox entry for a row that was never written.
 */
@Dao
abstract class LedgerWriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertTransactions(transactions: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertOutbox(entry: SyncOutboxEntity)

    @Query(
        "UPDATE transactions SET accountId = :accountId, categoryId = :categoryId, " +
            "type = :type, amountMinor = :amountMinor, currencyCode = :currencyCode, " +
            "occurredAt = :occurredAt, note = :note, updatedAt = :updatedAt, pendingPush = 1 " +
            "WHERE userId = :userId AND id = :id AND deletedAt IS NULL",
    )
    protected abstract suspend fun updateRow(
        userId: String,
        id: String,
        accountId: String,
        categoryId: String?,
        type: String,
        amountMinor: Long,
        currencyCode: String,
        occurredAt: Long,
        note: String?,
        updatedAt: Long,
    ): Int

    @Query(
        "UPDATE transactions SET deletedAt = :deletedAt, updatedAt = :deletedAt, pendingPush = 1 " +
            "WHERE userId = :userId AND id = :id AND deletedAt IS NULL",
    )
    protected abstract suspend fun deleteRow(userId: String, id: String, deletedAt: Long): Int

    @Query(
        "UPDATE transactions SET deletedAt = :deletedAt, updatedAt = :deletedAt " +
            "WHERE userId = :userId AND transferGroupId = :transferGroupId " +
            "AND deletedAt IS NULL",
    )
    protected abstract suspend fun deleteTransferRows(
        userId: String,
        transferGroupId: String,
        deletedAt: Long,
    ): Int

    @Query(
        "UPDATE transactions SET deletedAt = NULL, updatedAt = :updatedAt, pendingPush = 1 " +
            "WHERE userId = :userId AND id = :id AND deletedAt IS NOT NULL",
    )
    protected abstract suspend fun restoreRow(userId: String, id: String, updatedAt: Long): Int

    @Transaction
    open suspend fun createWithOutbox(transaction: TransactionEntity, entry: SyncOutboxEntity) {
        upsertTransaction(transaction)
        insertOutbox(entry)
    }

    /**
     * Both transfer legs in one Room transaction: a failure on either insert
     * leaves neither row. No outbox entry — transfers are device-local until
     * the API models them.
     */
    @Transaction
    open suspend fun createTransfer(legs: List<TransactionEntity>) {
        upsertTransactions(legs)
    }

    /** Tombstones every live leg of the group; the caller does not queue deletes. */
    open suspend fun deleteTransferGroup(
        userId: String,
        transferGroupId: String,
        deletedAt: Long,
    ): Int = deleteTransferRows(userId, transferGroupId, deletedAt)

    @Transaction
    open suspend fun updateWithOutbox(
        userId: String,
        id: String,
        accountId: String,
        categoryId: String?,
        type: String,
        amountMinor: Long,
        currencyCode: String,
        occurredAt: Long,
        note: String?,
        updatedAt: Long,
        entry: SyncOutboxEntity,
    ): Int {
        val changed = updateRow(
            userId = userId,
            id = id,
            accountId = accountId,
            categoryId = categoryId,
            type = type,
            amountMinor = amountMinor,
            currencyCode = currencyCode,
            occurredAt = occurredAt,
            note = note,
            updatedAt = updatedAt,
        )
        if (changed > 0) insertOutbox(entry)
        return changed
    }

    @Transaction
    open suspend fun deleteWithOutbox(
        userId: String,
        id: String,
        deletedAt: Long,
        entry: SyncOutboxEntity,
    ): Int {
        val changed = deleteRow(userId, id, deletedAt)
        if (changed > 0) insertOutbox(entry)
        return changed
    }

    @Transaction
    open suspend fun restoreWithOutbox(
        userId: String,
        id: String,
        updatedAt: Long,
        entry: SyncOutboxEntity,
    ): Int {
        val changed = restoreRow(userId, id, updatedAt)
        if (changed > 0) insertOutbox(entry)
        return changed
    }
}

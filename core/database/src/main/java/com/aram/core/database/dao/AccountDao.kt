package com.aram.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aram.core.database.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(accounts: List<AccountEntity>)

    @Query(
        "SELECT * FROM accounts WHERE userId = :userId AND deletedAt IS NULL " +
            "ORDER BY sortOrder ASC",
    )
    fun observeByUser(userId: String): Flow<List<AccountEntity>>

    @Query(
        "SELECT * FROM accounts WHERE userId = :userId AND id = :id " +
            "AND deletedAt IS NULL",
    )
    suspend fun findById(userId: String, id: String): AccountEntity?

    @Query("SELECT COUNT(*) FROM accounts WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun countByUser(userId: String): Int

    /**
     * Opening balance plus the account's non-deleted ledger rows. TRANSFER rows
     * are signed (negative leaves the account, positive arrives); rows in a
     * different currency than the account are ignored instead of being folded
     * into the sum. Emits null for an unknown or tombstoned account.
     */
    @Query(
        "SELECT a.id AS accountId, a.currencyCode AS currencyCode, " +
            "a.openingBalanceMinor + COALESCE((" +
            "SELECT SUM(CASE t.type " +
            "WHEN 'INCOME' THEN t.amountMinor " +
            "WHEN 'EXPENSE' THEN -t.amountMinor " +
            "WHEN 'TRANSFER' THEN t.amountMinor " +
            "ELSE 0 END) " +
            "FROM transactions t " +
            "WHERE t.userId = a.userId AND t.accountId = a.id " +
            "AND t.deletedAt IS NULL AND t.currencyCode = a.currencyCode" +
            "), 0) AS balanceMinor " +
            "FROM accounts a " +
            "WHERE a.userId = :userId AND a.id = :accountId AND a.deletedAt IS NULL",
    )
    fun observeBalance(userId: String, accountId: String): Flow<AccountBalanceRow?>

    /** Drops local-only accounts (e.g. the offline seed) once the server list is in. */
    @Query(
        "UPDATE accounts SET deletedAt = :deletedAt, updatedAt = :deletedAt " +
            "WHERE userId = :userId AND deletedAt IS NULL AND id NOT IN (:keepIds) " +
            "AND NOT EXISTS (SELECT 1 FROM transactions t " +
            "WHERE t.accountId = accounts.id AND t.deletedAt IS NULL)",
    )
    suspend fun softDeleteOrphans(userId: String, keepIds: List<String>, deletedAt: Long): Int

    @Query("DELETE FROM accounts WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)
}

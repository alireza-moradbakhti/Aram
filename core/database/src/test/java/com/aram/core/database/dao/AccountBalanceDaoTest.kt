package com.aram.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aram.core.database.AramDatabase
import com.aram.core.database.entity.AccountEntity
import com.aram.core.database.entity.TransactionEntity
import com.aram.core.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AccountBalanceDaoTest {

    private lateinit var db: AramDatabase
    private lateinit var accounts: AccountDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AramDatabase::class.java).build()
        accounts = db.accountDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `account without transactions returns its opening balance`() = runBlocking {
        insert(account("a1", opening = 5_000L))

        assertEquals(5_000L, balance("a1"))
    }

    @Test
    fun `income adds and expense subtracts`() = runBlocking {
        insert(
            account("a1", opening = 1_000L),
            transaction("t1", "a1", 500L, TransactionType.INCOME),
            transaction("t2", "a1", 1_300L, TransactionType.EXPENSE),
        )

        assertEquals(200L, balance("a1"))
    }

    @Test
    fun `expenses beyond the opening balance yield a negative balance`() = runBlocking {
        insert(
            account("a1", opening = 1_000L),
            transaction("t1", "a1", 2_500L, TransactionType.EXPENSE),
        )

        assertEquals(-1_500L, balance("a1"))
    }

    @Test
    fun `transfer legs move both accounts by the same amount`() = runBlocking {
        insert(
            account("a1", opening = 10_000L),
            account("a2", opening = 0L),
            transaction("out", "a1", -3_000L, TransactionType.TRANSFER, group = "g1"),
            transaction("in", "a2", 3_000L, TransactionType.TRANSFER, group = "g1"),
        )

        assertEquals(7_000L, balance("a1"))
        assertEquals(3_000L, balance("a2"))
    }

    @Test
    fun `a transfer between the same account nets to zero`() = runBlocking {
        insert(
            account("a1", opening = 10_000L),
            transaction("out", "a1", -2_000L, TransactionType.TRANSFER, group = "g1"),
            transaction("in", "a1", 2_000L, TransactionType.TRANSFER, group = "g1"),
        )

        assertEquals(10_000L, balance("a1"))
    }

    @Test
    fun `transfer legs are not counted as income or expense`() = runBlocking {
        insert(
            account("a1", opening = 0L),
            transaction("out", "a1", -3_000L, TransactionType.TRANSFER, group = "g1"),
            transaction("in", "a1", 500L, TransactionType.TRANSFER, group = "g1"),
        )

        assertEquals(-2_500L, balance("a1"))
    }

    @Test
    fun `soft deleted rows are ignored`() = runBlocking {
        insert(
            account("a1", opening = 1_000L),
            transaction("t1", "a1", 900L, TransactionType.EXPENSE, deletedAt = 9L),
        )

        assertEquals(1_000L, balance("a1"))
    }

    @Test
    fun `rows in another currency are ignored`() = runBlocking {
        insert(
            account("a1", opening = 1_000L, currency = "IRR"),
            transaction("t1", "a1", 900L, TransactionType.EXPENSE, currency = "USD"),
        )

        assertEquals(1_000L, balance("a1"))
    }

    @Test
    fun `rows of another user are ignored`() = runBlocking {
        insert(
            account("a1", opening = 1_000L),
            transaction("t1", "a1", 900L, TransactionType.EXPENSE, userId = "other"),
        )

        assertEquals(1_000L, balance("a1"))
    }

    @Test
    fun `unknown or tombstoned accounts have no balance`() = runBlocking {
        insert(account("gone", opening = 1_000L, deletedAt = 5L))

        assertNull(accounts.observeBalance(USER, "missing").first())
        assertNull(accounts.observeBalance(USER, "gone").first())
    }

    private suspend fun insert(vararg entities: Any) = with(db) {
        accountDao().upsertAll(entities.filterIsInstance<AccountEntity>())
        transactionDao().upsertAll(entities.filterIsInstance<TransactionEntity>())
    }

    private suspend fun balance(accountId: String): Long? =
        accounts.observeBalance(USER, accountId).first()?.balanceMinor

    private fun account(
        id: String,
        opening: Long = 0L,
        currency: String = "IRR",
        userId: String = USER,
        deletedAt: Long? = null,
    ) = AccountEntity(
        id = id,
        userId = userId,
        name = id,
        type = "CASH",
        currencyCode = currency,
        openingBalanceMinor = opening,
        colorArgb = 0,
        iconKey = "wallet",
        sortOrder = 0,
        archived = false,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = deletedAt,
    )

    private fun transaction(
        id: String,
        accountId: String,
        amount: Long,
        type: TransactionType,
        currency: String = "IRR",
        userId: String = USER,
        group: String? = null,
        deletedAt: Long? = null,
    ) = TransactionEntity(
        id = id,
        userId = userId,
        type = type.name,
        accountId = accountId,
        categoryId = null,
        amountMinor = amount,
        currencyCode = currency,
        occurredAt = 0L,
        note = null,
        transferGroupId = group,
        recurringRuleId = null,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = deletedAt,
    )

    private companion object {
        const val USER = "user-1"
    }
}
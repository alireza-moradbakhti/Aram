package com.aram.core.data.repository

import com.aram.core.common.CoroutineDispatchers
import com.aram.core.database.dao.AccountDao
import com.aram.core.database.dao.CategoryDao
import com.aram.core.database.dao.TransactionDao
import com.aram.core.database.entity.AccountEntity
import com.aram.core.database.entity.CategoryEntity
import com.aram.core.database.entity.TransactionEntity
import com.aram.core.datastore.SessionStore
import com.aram.core.domain.transaction.TransactionDraft
import com.aram.core.logging.Logger
import com.aram.core.model.AccountType
import com.aram.core.model.AppError
import com.aram.core.model.CategoryKind
import com.aram.core.model.Outcome
import com.aram.core.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionRepositoryImplTest {

    private val transactionDao = FakeTransactionDao()
    private val accountDao = FakeAccountDao()
    private val categoryDao = FakeCategoryDao()
    private val session = FakeSessionStore()
    private val repository = TransactionRepositoryImpl(
        transactionDao = transactionDao,
        accountDao = accountDao,
        categoryDao = categoryDao,
        session = session,
        dispatchers = TestDispatchers,
        logger = Logger(),
    )

    @Test
    fun `create scopes the row to the session user with timestamps`() = runTest {
        val before = System.currentTimeMillis()

        val result = repository.create(draft())

        assertTrue(result is Outcome.Success)
        val row = transactionDao.rows.values.single()
        assertEquals("user-1", row.userId)
        assertEquals(1_000L, row.amountMinor)
        assertEquals(TransactionType.EXPENSE.name, row.type)
        assertNull(row.deletedAt)
        assertTrue(row.createdAt >= before)
        assertEquals(row.createdAt, row.updatedAt)
    }

    @Test
    fun `create stores a blank note as null`() = runTest {
        repository.create(draft(note = "   "))

        assertNull(transactionDao.rows.values.single().note)
    }

    @Test
    fun `create without a session is unauthorized`() = runTest {
        session.userId.value = null

        val result = repository.create(draft())

        assertEquals(AppError.Unauthorized, (result as Outcome.Failure).error)
        assertTrue(transactionDao.rows.isEmpty())
    }

    @Test
    fun `update rewrites fields and bumps updatedAt`() = runTest {
        val id = (repository.create(draft()) as Outcome.Success).value.id
        val original = transactionDao.rows.getValue(id)

        val result = repository.update(id, draft(amountMinor = 5_000L, note = " lunch "))

        assertTrue(result is Outcome.Success)
        val row = transactionDao.rows.getValue(id)
        assertEquals(5_000L, row.amountMinor)
        assertEquals("lunch", row.note)
        assertEquals(original.createdAt, row.createdAt)
        assertTrue(row.updatedAt >= original.updatedAt)
    }

    @Test
    fun `update of an unknown row is not found`() = runTest {
        val result = repository.update("missing", draft())

        assertEquals(AppError.NotFound, (result as Outcome.Failure).error)
    }

    @Test
    fun `soft delete keeps the row and stamps the tombstone`() = runTest {
        val id = (repository.create(draft()) as Outcome.Success).value.id

        val result = repository.softDelete(id)

        assertTrue(result is Outcome.Success)
        val row = transactionDao.rows.getValue(id)
        assertNotNull(row.deletedAt)
        assertEquals(row.deletedAt, row.updatedAt)
    }

    @Test
    fun `soft delete of an unknown row is not found`() = runTest {
        val result = repository.softDelete("missing")

        assertEquals(AppError.NotFound, (result as Outcome.Failure).error)
    }

    @Test
    fun `observeTransactions returns the requested range newest first`() = runTest {
        transactionDao.rows["a"] = row("a", occurredAt = 1_000L)
        transactionDao.rows["b"] = row("b", occurredAt = 2_000L)
        transactionDao.rows["c"] = row("c", occurredAt = 3_000L)
        transactionDao.rows["gone"] = row("gone", occurredAt = 2_500L, deletedAt = 9L)

        val result = repository.observeTransactions(fromInclusive = 1_500L, toExclusive = 3_000L).first()

        assertEquals(listOf("b"), result.map { it.id })
    }

    @Test
    fun `observeRecent passes the limit and includes tombstones out`() = runTest {
        repeat(3) { index -> transactionDao.rows["t$index"] = row("t$index", occurredAt = index.toLong()) }

        val result = repository.observeRecent(limit = 2).first()

        assertEquals(2, result.size)
        assertEquals(2, transactionDao.lastLimit)
    }

    @Test
    fun `observation without a session emits empty`() = runTest {
        session.userId.value = null
        transactionDao.rows["a"] = row("a", occurredAt = 1_000L)

        val result = repository.observeTransactions().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `ledger lookup returns mapped rows and null when absent`() = runTest {
        accountDao.rows["acc"] = AccountEntity(
            id = "acc",
            userId = "user-1",
            name = "Cash",
            type = AccountType.CASH.name,
            currencyCode = "IRR",
            openingBalanceMinor = 0L,
            colorArgb = 0,
            iconKey = "wallet",
            sortOrder = 0,
            archived = false,
            createdAt = 0L,
            updatedAt = 0L,
            deletedAt = null,
        )
        categoryDao.rows["cat"] = CategoryEntity(
            id = "cat",
            userId = "user-1",
            name = "Food",
            kind = CategoryKind.EXPENSE.name,
            parentId = null,
            colorArgb = 0,
            iconKey = "food",
            sortOrder = 0,
            archived = false,
            createdAt = 0L,
            updatedAt = 0L,
            deletedAt = null,
        )

        val account = (repository.account("acc") as Outcome.Success).value
        val category = (repository.category("cat") as Outcome.Success).value
        val missing = (repository.category("missing") as Outcome.Success).value

        assertEquals("Cash", account?.name)
        assertEquals(CategoryKind.EXPENSE, category?.kind)
        assertNull(missing)
    }

    private fun draft(
        amountMinor: Long = 1_000L,
        note: String? = null,
    ) = TransactionDraft(
        accountId = "acc",
        categoryId = "cat",
        type = TransactionType.EXPENSE,
        amountMinor = amountMinor,
        currencyCode = "IRR",
        occurredAt = 1_700_000_000_000L,
        note = note,
    )

    private fun row(
        id: String,
        occurredAt: Long,
        deletedAt: Long? = null,
    ) = TransactionEntity(
        id = id,
        userId = "user-1",
        type = TransactionType.EXPENSE.name,
        accountId = "acc",
        categoryId = "cat",
        amountMinor = 1_000L,
        currencyCode = "IRR",
        occurredAt = occurredAt,
        note = null,
        transferGroupId = null,
        recurringRuleId = null,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = deletedAt,
    )
}

private object TestDispatchers : CoroutineDispatchers {
    override val io = Dispatchers.Unconfined
    override val default = Dispatchers.Unconfined
    override val main = Dispatchers.Unconfined
}

private class FakeSessionStore : SessionStore {
    override val userId = MutableStateFlow<String?>("user-1")
    override suspend fun setUserId(userId: String) {
        this.userId.value = userId
    }

    override suspend fun clear() {
        userId.value = null
    }
}

private class FakeTransactionDao : TransactionDao {
    val rows = linkedMapOf<String, TransactionEntity>()
    var lastLimit: Int? = null

    override suspend fun upsert(transaction: TransactionEntity) {
        rows[transaction.id] = transaction
    }

    override fun observeRange(
        userId: String,
        fromInclusive: Long,
        toExclusive: Long,
    ): Flow<List<TransactionEntity>> = flowOf(
        rows.values
            .filter { it.userId == userId && it.deletedAt == null && it.occurredAt in fromInclusive until toExclusive }
            .sortedByDescending { it.occurredAt },
    )

    override fun observeRecent(userId: String, limit: Int): Flow<List<TransactionEntity>> {
        lastLimit = limit
        return flowOf(
            rows.values
                .filter { it.userId == userId && it.deletedAt == null }
                .sortedByDescending { it.occurredAt }
                .take(limit),
        )
    }

    override fun observeById(userId: String, id: String): Flow<TransactionEntity?> =
        flowOf(rows[id]?.takeIf { it.userId == userId && it.deletedAt == null })

    override suspend fun findById(userId: String, id: String): TransactionEntity? =
        rows[id]?.takeIf { it.userId == userId && it.deletedAt == null }

    override suspend fun update(
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
    ): Int {
        val current = rows[id] ?: return 0
        if (current.userId != userId || current.deletedAt != null) return 0
        rows[id] = current.copy(
            accountId = accountId,
            categoryId = categoryId,
            type = type,
            amountMinor = amountMinor,
            currencyCode = currencyCode,
            occurredAt = occurredAt,
            note = note,
            updatedAt = updatedAt,
        )
        return 1
    }

    override suspend fun softDelete(userId: String, id: String, deletedAt: Long): Int {
        val current = rows[id] ?: return 0
        if (current.userId != userId || current.deletedAt != null) return 0
        rows[id] = current.copy(deletedAt = deletedAt, updatedAt = deletedAt)
        return 1
    }

    override fun observeTotal(
        userId: String,
        type: String,
        fromInclusive: Long,
        toExclusive: Long,
    ): Flow<Long> = flowOf(0L)

    override suspend fun deleteByUser(userId: String) {
        rows.entries.removeAll { it.value.userId == userId }
    }
}

private class FakeAccountDao : AccountDao {
    val rows = mutableMapOf<String, AccountEntity>()

    override suspend fun upsertAll(accounts: List<AccountEntity>) {
        accounts.forEach { rows[it.id] = it }
    }

    override fun observeByUser(userId: String): Flow<List<AccountEntity>> =
        flowOf(rows.values.filter { it.userId == userId && it.deletedAt == null })

    override suspend fun findById(userId: String, id: String): AccountEntity? =
        rows[id]?.takeIf { it.userId == userId && it.deletedAt == null }

    override suspend fun countByUser(userId: String): Int =
        rows.values.count { it.userId == userId && it.deletedAt == null }

    override suspend fun deleteByUser(userId: String) {
        rows.entries.removeAll { it.value.userId == userId }
    }
}

private class FakeCategoryDao : CategoryDao {
    val rows = mutableMapOf<String, CategoryEntity>()

    override suspend fun upsertAll(categories: List<CategoryEntity>) {
        categories.forEach { rows[it.id] = it }
    }

    override fun observeByUser(userId: String): Flow<List<CategoryEntity>> =
        flowOf(rows.values.filter { it.userId == userId && it.deletedAt == null })

    override suspend fun findById(userId: String, id: String): CategoryEntity? =
        rows[id]?.takeIf { it.userId == userId && it.deletedAt == null }

    override suspend fun countByUser(userId: String): Int =
        rows.values.count { it.userId == userId && it.deletedAt == null }

    override suspend fun deleteByUser(userId: String) {
        rows.entries.removeAll { it.value.userId == userId }
    }
}

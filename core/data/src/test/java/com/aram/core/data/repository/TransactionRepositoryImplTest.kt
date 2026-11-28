package com.aram.core.data.repository

import com.aram.core.common.CoroutineDispatchers
import com.aram.core.data.FakeAccountDao
import com.aram.core.data.FakeCategoryDao
import com.aram.core.data.FakeLedgerWriteDao
import com.aram.core.data.FakeOutboxTrigger
import com.aram.core.data.FakeSyncOutboxDao
import com.aram.core.data.FakeTransactionDao
import com.aram.core.database.dao.AccountDao
import com.aram.core.database.dao.CategoryDao
import com.aram.core.database.dao.TransactionDao
import com.aram.core.database.entity.AccountEntity
import com.aram.core.database.entity.CategoryEntity
import com.aram.core.database.entity.SyncOutboxEntity
import com.aram.core.database.entity.TransactionEntity
import com.aram.core.datastore.SessionStore
import com.aram.core.domain.transaction.TransactionDraft
import com.aram.core.domain.transaction.TransactionFilter
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
    private val outbox = FakeSyncOutboxDao()
    private val trigger = FakeOutboxTrigger()
    private val repository = TransactionRepositoryImpl(
        transactionDao = transactionDao,
        accountDao = accountDao,
        categoryDao = categoryDao,
        writeDao = FakeLedgerWriteDao(transactionDao, outbox),
        outboxTrigger = trigger,
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
    fun `create queues a push and schedules the worker`() = runTest {
        val id = (repository.create(draft()) as Outcome.Success).value.id

        val entry = outbox.rows.values.single()
        assertEquals(SyncOutboxEntity.OP_CREATE, entry.op)
        assertEquals(id, entry.entityId)
        assertTrue(entry.payload?.contains(id) == true)
        assertEquals(1, trigger.scheduled)
    }

    @Test
    fun `update and delete queue their own operations`() = runTest {
        val id = (repository.create(draft()) as Outcome.Success).value.id

        repository.update(id, draft(amountMinor = 2_000L))
        repository.softDelete(id)

        assertEquals(
            listOf(SyncOutboxEntity.OP_CREATE, SyncOutboxEntity.OP_UPDATE, SyncOutboxEntity.OP_DELETE),
            outbox.rows.values.map { it.op },
        )
        assertEquals(3, trigger.scheduled)
    }

    @Test
    fun `failed writes do not queue anything`() = runTest {
        repository.update("missing", draft())

        assertTrue(outbox.rows.isEmpty())
        assertEquals(0, trigger.scheduled)
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
    fun `search trims the query and forwards every filter`() = runTest {
        transactionDao.rows["a"] = row("a", occurredAt = 1_500L, note = "Lunch box")

        val result = repository.search(
            TransactionFilter(
                query = "  lunch  ",
                type = TransactionType.EXPENSE,
                categoryId = "cat",
                fromInclusive = 1_000L,
                toExclusive = 2_000L,
            ),
        ).first()

        assertEquals(listOf("a"), result.map { it.id })
        assertEquals("lunch", transactionDao.lastFilter?.query)
        assertEquals(TransactionType.EXPENSE, transactionDao.lastFilter?.type)
        assertEquals("cat", transactionDao.lastFilter?.categoryId)
        assertEquals(1_000L, transactionDao.lastFilter?.fromInclusive)
        assertEquals(2_000L, transactionDao.lastFilter?.toExclusive)
    }

    @Test
    fun `search without a session emits empty`() = runTest {
        session.userId.value = null
        transactionDao.rows["a"] = row("a", occurredAt = 1_500L)

        assertTrue(repository.search(TransactionFilter(query = "a")).first().isEmpty())
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
        note: String? = null,
    ) = TransactionEntity(
        id = id,
        userId = "user-1",
        type = TransactionType.EXPENSE.name,
        accountId = "acc",
        categoryId = "cat",
        amountMinor = 1_000L,
        currencyCode = "IRR",
        occurredAt = occurredAt,
        note = note,
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

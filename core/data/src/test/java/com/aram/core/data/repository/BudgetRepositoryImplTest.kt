package com.aram.core.data.repository

import com.aram.core.common.CoroutineDispatchers
import com.aram.core.data.FakeBudgetDao
import com.aram.core.data.FakeCategoryDao
import com.aram.core.data.FakeSessionStore
import com.aram.core.data.FakeTransactionDao
import com.aram.core.database.entity.CategoryEntity
import com.aram.core.database.entity.TransactionEntity
import com.aram.core.domain.budget.BudgetDraft
import com.aram.core.logging.Logger
import com.aram.core.model.AppError
import com.aram.core.model.CategoryKind
import com.aram.core.model.Outcome
import com.aram.core.model.TransactionType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetRepositoryImplTest {

    private val budgetDao = FakeBudgetDao()
    private val categoryDao = FakeCategoryDao()
    private val transactionDao = FakeTransactionDao()
    private val session = FakeSessionStore(initialUserId = "user-1")
    private val clock = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC)
    private val repository = BudgetRepositoryImpl(
        budgetDao = budgetDao,
        categoryDao = categoryDao,
        transactionDao = transactionDao,
        session = session,
        clock = clock,
        dispatchers = BudgetTestDispatchers,
        logger = Logger(),
    )

    private val november = YearMonth.of(2026, 11)
    private val novemberStartEpochDay = LocalDate.of(2026, 11, 1).toEpochDay()

    @Test
    fun `upsert stores one row per category and month`() = runTest {
        givenCategory("groceries")

        val first = repository.upsert(draft(limitMinor = 6_000_000))
        assertTrue(first is Outcome.Success)

        val created = budgetDao.rows.values.single()
        assertEquals("user-1", created.userId)
        assertEquals("groceries", created.categoryId)
        assertEquals(novemberStartEpochDay, created.periodStartEpochDay)
        assertEquals("IRR", created.currencyCode)
        assertNull(created.deletedAt)
        assertEquals(created.createdAt, created.updatedAt)

        val second = repository.upsert(draft(limitMinor = 7_500_000))
        assertTrue(second is Outcome.Success)

        val replaced = budgetDao.rows.values.single()
        assertEquals(created.id, replaced.id)
        assertEquals(7_500_000L, replaced.limitMinor)
        assertEquals(created.createdAt, replaced.createdAt)
        assertTrue(replaced.updatedAt >= created.updatedAt)
    }

    @Test
    fun `re-creating a deleted budget revives the same row`() = runTest {
        givenCategory("groceries")
        repository.upsert(draft(limitMinor = 6_000_000))
        val id = budgetDao.rows.keys.single()

        val deleted = repository.delete(id)
        assertTrue(deleted is Outcome.Success)
        assertNotNull(budgetDao.rows.getValue(id).deletedAt)

        val revived = repository.upsert(draft(limitMinor = 4_000_000))
        assertTrue(revived is Outcome.Success)

        val row = budgetDao.rows.values.single()
        assertEquals(id, row.id)
        assertNull(row.deletedAt)
        assertEquals(4_000_000L, row.limitMinor)
    }

    @Test
    fun `progress sums only the month's expenses in the budget currency`() = runTest {
        givenCategory("groceries")
        givenCategory("dining")
        repository.upsert(draft(categoryId = "groceries", limitMinor = 6_000_000))

        transactionDao.upsert(expense("groceries", 1_450_000, "2026-11-10T09:00:00Z"))
        transactionDao.upsert(expense("dining", 700_000, "2026-11-12T12:00:00Z"))
        transactionDao.upsert(expense("groceries", 200_000, "2026-11-13T08:00:00Z", currency = "USD"))
        transactionDao.upsert(expense("groceries", 900_000, "2026-11-14T08:00:00Z", deleted = true))
        transactionDao.upsert(expense("groceries", 500_000, "2026-12-01T00:00:00Z"))
        transactionDao.upsert(expense("groceries", 300_000, "2026-10-31T23:59:59.999Z"))
        transactionDao.upsert(
            row(TransactionType.INCOME, "groceries", 3_000_000, "2026-11-05T08:00:00Z"),
        )
        transactionDao.upsert(
            row(TransactionType.TRANSFER, "groceries", 1_000_000, "2026-11-06T08:00:00Z"),
        )

        val progress = repository.observeProgress(november).first().single()

        assertEquals("groceries", progress.category.id)
        assertEquals(6_000_000L, progress.limitMinor)
        assertEquals(1_450_000L, progress.spentMinor)
        assertEquals(4_550_000L, progress.remainingMinor)
        assertFalse(progress.isOver)
        assertEquals(1_450_000f / 6_000_000f, progress.fraction, 0.0001f)
    }

    @Test
    fun `month boundaries are half-open in the clock zone`() = runTest {
        givenCategory("groceries")
        repository.upsert(draft(limitMinor = 10_000_000))
        transactionDao.upsert(expense("groceries", 100_000, "2026-11-01T00:00:00Z"))
        transactionDao.upsert(expense("groceries", 200_000, "2026-11-30T23:59:59.999Z"))
        transactionDao.upsert(expense("groceries", 400_000, "2026-12-01T00:00:00Z"))
        transactionDao.upsert(expense("groceries", 800_000, "2026-10-31T23:59:59.999Z"))

        val progress = repository.observeProgress(november).first().single()

        assertEquals(300_000L, progress.spentMinor)
    }

    @Test
    fun `archived categories are hidden but their budget row is kept`() = runTest {
        givenCategory("groceries")
        repository.upsert(draft(limitMinor = 6_000_000))
        categoryDao.rows["groceries"] = categoryDao.rows.getValue("groceries").copy(archived = true)

        val progress = repository.observeProgress(november).first()

        assertTrue(progress.isEmpty())
        assertEquals(1, budgetDao.rows.size)
        assertNull(budgetDao.rows.values.single().deletedAt)
    }

    @Test
    fun `upsert rejects income and archived categories`() = runTest {
        givenCategory("salary", kind = CategoryKind.INCOME)
        givenCategory("groceries", archived = true)

        val salary = repository.upsert(draft(categoryId = "salary", limitMinor = 1_000_000))
        val archived = repository.upsert(draft(categoryId = "groceries", limitMinor = 1_000_000))

        assertTrue(salary is Outcome.Failure && salary.error is AppError.Validation)
        assertTrue(archived is Outcome.Failure && archived.error is AppError.Validation)
        assertTrue(budgetDao.rows.isEmpty())
    }

    @Test
    fun `upsert rejects non-positive limits and unknown currencies`() = runTest {
        givenCategory("groceries")

        val zero = repository.upsert(draft(limitMinor = 0))
        val negative = repository.upsert(draft(limitMinor = -5))
        val currency = repository.upsert(draft(limitMinor = 1_000, currency = "XXX"))

        assertTrue(zero is Outcome.Failure && zero.error is AppError.Validation)
        assertTrue(negative is Outcome.Failure && negative.error is AppError.Validation)
        assertTrue(currency is Outcome.Failure && currency.error is AppError.Validation)
        assertTrue(budgetDao.rows.isEmpty())
    }

    @Test
    fun `delete tombstones the row and hides it from progress`() = runTest {
        givenCategory("groceries")
        repository.upsert(draft(limitMinor = 6_000_000))
        val id = budgetDao.rows.keys.single()

        val deleted = repository.delete(id)
        assertTrue(deleted is Outcome.Success)
        assertNotNull(budgetDao.rows.getValue(id).deletedAt)
        assertTrue(repository.observeProgress(november).first().isEmpty())

        val missing = repository.delete(id)
        assertTrue(missing is Outcome.Failure && missing.error == AppError.NotFound)
    }

    @Test
    fun `no session yields no progress and unauthorized writes`() = runTest {
        givenCategory("groceries")
        session.clear()

        assertTrue(repository.observeProgress(november).first().isEmpty())

        val result = repository.upsert(draft(limitMinor = 6_000_000))
        assertTrue(result is Outcome.Failure && result.error == AppError.Unauthorized)
    }

    private fun draft(
        categoryId: String = "groceries",
        limitMinor: Long = 6_000_000,
        currency: String = "IRR",
    ) = BudgetDraft(
        categoryId = categoryId,
        month = november,
        limitMinor = limitMinor,
        currencyCode = currency,
    )

    private fun givenCategory(
        id: String,
        kind: CategoryKind = CategoryKind.EXPENSE,
        archived: Boolean = false,
    ) {
        categoryDao.rows[id] = CategoryEntity(
            id = id,
            userId = "user-1",
            name = id.replaceFirstChar { it.uppercase() },
            kind = kind.name,
            parentId = null,
            colorArgb = 0,
            iconKey = "wallet",
            sortOrder = 0,
            archived = archived,
            createdAt = 0L,
            updatedAt = 0L,
            deletedAt = null,
        )
    }

    private fun expense(
        categoryId: String,
        amountMinor: Long,
        instant: String,
        currency: String = "IRR",
        deleted: Boolean = false,
    ) = row(TransactionType.EXPENSE, categoryId, amountMinor, instant, currency, deleted)

    private fun row(
        type: TransactionType,
        categoryId: String,
        amountMinor: Long,
        instant: String,
        currency: String = "IRR",
        deleted: Boolean = false,
    ) = TransactionEntity(
        id = "tx-${type.name}-$categoryId-$amountMinor-$instant-$currency",
        userId = "user-1",
        type = type.name,
        accountId = "cash",
        categoryId = categoryId,
        amountMinor = amountMinor,
        currencyCode = currency,
        occurredAt = Instant.parse(instant).toEpochMilli(),
        note = null,
        transferGroupId = null,
        recurringRuleId = null,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = if (deleted) 1L else null,
        pendingPush = false,
    )
}

private object BudgetTestDispatchers : CoroutineDispatchers {
    override val io = Dispatchers.Unconfined
    override val default = Dispatchers.Unconfined
    override val main = Dispatchers.Unconfined
}
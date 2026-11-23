package com.aram.core.domain.usecase

import com.aram.core.domain.transaction.FakeLedgerLookup
import com.aram.core.domain.transaction.FakeTransactionRepository
import com.aram.core.domain.transaction.TransactionField
import com.aram.core.domain.transaction.TransactionProblem
import com.aram.core.domain.transaction.TransactionWriteResult
import com.aram.core.domain.transaction.testAccount
import com.aram.core.domain.transaction.testCategory
import com.aram.core.domain.transaction.testDraft
import com.aram.core.model.AppError
import com.aram.core.model.Outcome
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateTransactionTest {

    private val repository = FakeTransactionRepository()
    private val lookup = FakeLedgerLookup().apply {
        accounts["acc"] = testAccount()
        categories["cat"] = testCategory()
    }
    private val create = CreateTransaction(repository, lookup)

    @Test
    fun `valid draft is written`() = runTest {
        val result = create(testDraft())

        assertTrue(result is TransactionWriteResult.Success)
        assertEquals(1, repository.created.size)
        assertEquals(1_000L, repository.created.single().amountMinor)
    }

    @Test
    fun `invalid amount never reaches the repository`() = runTest {
        val result = create(testDraft(amountMinor = 0L))

        assertTrue(result is TransactionWriteResult.Invalid)
        assertEquals(
            TransactionProblem.AmountNotPositive,
            (result as TransactionWriteResult.Invalid).problems[TransactionField.AMOUNT],
        )
        assertTrue(repository.created.isEmpty())
    }

    @Test
    fun `unknown account is reported on the account field`() = runTest {
        lookup.accounts.clear()

        val result = create(testDraft())

        assertTrue(result is TransactionWriteResult.Invalid)
        assertEquals(
            TransactionProblem.AccountNotFound,
            (result as TransactionWriteResult.Invalid).problems[TransactionField.ACCOUNT],
        )
        assertTrue(repository.created.isEmpty())
    }

    @Test
    fun `lookup storage failure is surfaced as a failed write`() = runTest {
        lookup.accountResult = Outcome.Failure(AppError.Storage())

        val result = create(testDraft())

        assertTrue(result is TransactionWriteResult.Failed)
        assertTrue((result as TransactionWriteResult.Failed).error is AppError.Storage)
        assertTrue(repository.created.isEmpty())
    }

    @Test
    fun `repository failure is surfaced as a failed write`() = runTest {
        repository.createResult = { Outcome.Failure(AppError.Conflict()) }

        val result = create(testDraft())

        assertTrue(result is TransactionWriteResult.Failed)
    }
}

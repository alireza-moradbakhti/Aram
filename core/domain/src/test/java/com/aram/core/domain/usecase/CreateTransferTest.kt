package com.aram.core.domain.usecase

import com.aram.core.domain.transaction.FakeLedgerLookup
import com.aram.core.domain.transaction.FakeTransactionRepository
import com.aram.core.domain.transaction.TransactionField
import com.aram.core.domain.transaction.TransferDraft
import com.aram.core.domain.transaction.TransferProblem
import com.aram.core.domain.transaction.TransferWriteResult
import com.aram.core.domain.transaction.testAccount
import com.aram.core.model.AppError
import com.aram.core.model.Outcome
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateTransferTest {

    private val repository = FakeTransactionRepository()
    private val lookup = FakeLedgerLookup().apply {
        accounts["cash"] = testAccount(id = "cash", currencyCode = "USD")
        accounts["card"] = testAccount(id = "card", currencyCode = "USD")
    }
    private val createTransfer = CreateTransfer(repository, lookup)

    @Test
    fun `valid transfer is written and returns the group id`() = runTest {
        val result = createTransfer(draft())

        assertTrue(result is TransferWriteResult.Success)
        assertEquals("transfer-group-1", (result as TransferWriteResult.Success).transferGroupId)
        assertEquals(1, repository.transfers.size)
        assertEquals("cash", repository.transfers.single().fromAccountId)
        assertEquals("card", repository.transfers.single().toAccountId)
    }

    @Test
    fun `same-account transfer never reaches the repository`() = runTest {
        val result = createTransfer(draft(toAccountId = "cash"))

        assertTrue(result is TransferWriteResult.Invalid)
        assertEquals(
            TransferProblem.SameAccount,
            (result as TransferWriteResult.Invalid).problems[TransactionField.TO_ACCOUNT],
        )
        assertTrue(repository.transfers.isEmpty())
    }

    @Test
    fun `unknown destination is reported on the destination field`() = runTest {
        val result = createTransfer(draft(toAccountId = "missing"))

        assertTrue(result is TransferWriteResult.Invalid)
        assertEquals(
            TransferProblem.AccountNotFound,
            (result as TransferWriteResult.Invalid).problems[TransactionField.TO_ACCOUNT],
        )
        assertTrue(repository.transfers.isEmpty())
    }

    @Test
    fun `cross-currency transfer is rejected`() = runTest {
        lookup.accounts["rial"] = testAccount(id = "rial", currencyCode = "IRR")

        val result = createTransfer(draft(toAccountId = "rial"))

        assertTrue(result is TransferWriteResult.Invalid)
        assertEquals(
            TransferProblem.CurrencyMismatch(sourceCurrency = "USD", destinationCurrency = "IRR"),
            (result as TransferWriteResult.Invalid).problems[TransactionField.CURRENCY],
        )
        assertTrue(repository.transfers.isEmpty())
    }

    @Test
    fun `lookup failure is surfaced as a failed write`() = runTest {
        lookup.accountResult = Outcome.Failure(AppError.Storage())

        val result = createTransfer(draft())

        assertTrue(result is TransferWriteResult.Failed)
        assertTrue((result as TransferWriteResult.Failed).error is AppError.Storage)
        assertTrue(repository.transfers.isEmpty())
    }

    @Test
    fun `repository failure is surfaced as a failed write`() = runTest {
        repository.transferResult = Outcome.Failure(AppError.Conflict())

        val result = createTransfer(draft())

        assertTrue(result is TransferWriteResult.Failed)
        assertTrue((result as TransferWriteResult.Failed).error is AppError.Conflict)
    }

    private fun draft(
        toAccountId: String = "card",
        amountMinor: Long = 125_000L,
    ) = TransferDraft(
        fromAccountId = "cash",
        toAccountId = toAccountId,
        amountMinor = amountMinor,
        currencyCode = "USD",
        occurredAt = 1_700_000_000_000L,
        note = "Move to card",
    )
}
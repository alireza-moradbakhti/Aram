package com.aram.core.domain.usecase

import com.aram.core.domain.transaction.FakeLedgerLookup
import com.aram.core.domain.transaction.FakeTransactionRepository
import com.aram.core.domain.transaction.TransactionField
import com.aram.core.domain.transaction.TransactionProblem
import com.aram.core.domain.transaction.TransactionWriteResult
import com.aram.core.domain.transaction.testAccount
import com.aram.core.domain.transaction.testCategory
import com.aram.core.domain.transaction.testDraft
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateTransactionTest {

    private val repository = FakeTransactionRepository()
    private val lookup = FakeLedgerLookup().apply {
        accounts["acc"] = testAccount()
        categories["cat"] = testCategory()
    }
    private val update = UpdateTransaction(repository, lookup)

    @Test
    fun `valid update is delegated with its id`() = runTest {
        val draft = testDraft(amountMinor = 2_500L)

        val result = update("tx-1", draft)

        assertTrue(result is TransactionWriteResult.Success)
        assertEquals(1, repository.updated.size)
        assertEquals("tx-1" to draft, repository.updated.single())
    }

    @Test
    fun `invalid update leaves the repository untouched`() = runTest {
        val result = update("tx-1", testDraft(amountMinor = -1L))

        assertTrue(result is TransactionWriteResult.Invalid)
        assertEquals(
            TransactionProblem.AmountNotPositive,
            (result as TransactionWriteResult.Invalid).problems[TransactionField.AMOUNT],
        )
        assertTrue(repository.updated.isEmpty())
    }
}

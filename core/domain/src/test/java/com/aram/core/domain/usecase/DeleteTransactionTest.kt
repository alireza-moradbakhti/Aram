package com.aram.core.domain.usecase

import com.aram.core.domain.transaction.FakeTransactionRepository
import com.aram.core.model.AppError
import com.aram.core.model.Outcome
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteTransactionTest {

    @Test
    fun `delegates to soft delete`() = runTest {
        val repository = FakeTransactionRepository()
        val result = DeleteTransaction(repository)("tx-1")

        assertEquals(listOf("tx-1"), repository.deleted)
        assertTrue(result is Outcome.Success)
    }

    @Test
    fun `propagates repository failures`() = runTest {
        val repository = FakeTransactionRepository().apply {
            deleteResult = Outcome.Failure(AppError.NotFound)
        }
        val result = DeleteTransaction(repository)("tx-1")

        assertEquals(AppError.NotFound, (result as Outcome.Failure).error)
    }
}

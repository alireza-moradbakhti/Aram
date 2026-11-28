package com.aram.core.domain.transaction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferValidatorTest {

    private val source = testAccount(id = "cash", currencyCode = "USD")
    private val destination = testAccount(id = "card", currencyCode = "USD")

    @Test
    fun `matching same-currency accounts pass`() {
        val result = TransferValidator.validate(draft(), source, destination)

        assertEquals(TransferValidation.Valid, result)
    }

    @Test
    fun `non-positive amount is reported on the amount field`() {
        val result = TransferValidator.validate(draft(amountMinor = 0L), source, destination)

        assertInvalid(result, TransactionField.AMOUNT, TransferProblem.AmountNotPositive)
    }

    @Test
    fun `transferring to the same account is reported on the destination`() {
        val result = TransferValidator.validate(
            draft(toAccountId = "cash"),
            source,
            source,
        )

        assertInvalid(result, TransactionField.TO_ACCOUNT, TransferProblem.SameAccount)
    }

    @Test
    fun `missing source is reported on the account field`() {
        val result = TransferValidator.validate(draft(), null, destination)

        assertInvalid(result, TransactionField.ACCOUNT, TransferProblem.AccountNotFound)
    }

    @Test
    fun `missing destination is reported on the destination field`() {
        val result = TransferValidator.validate(draft(), source, null)

        assertInvalid(result, TransactionField.TO_ACCOUNT, TransferProblem.AccountNotFound)
    }

    @Test
    fun `archived accounts are rejected on both sides`() {
        val archivedSource = TransferValidator.validate(
            draft(),
            testAccount(id = "cash", currencyCode = "USD", archived = true),
            destination,
        )
        val archivedDestination = TransferValidator.validate(
            draft(),
            source,
            testAccount(id = "card", currencyCode = "USD", archived = true),
        )

        assertInvalid(archivedSource, TransactionField.ACCOUNT, TransferProblem.AccountArchived)
        assertInvalid(
            archivedDestination,
            TransactionField.TO_ACCOUNT,
            TransferProblem.AccountArchived,
        )
    }

    @Test
    fun `cross-currency transfer names both currencies`() {
        val result = TransferValidator.validate(
            draft(currencyCode = "USD"),
            source,
            testAccount(id = "rial", currencyCode = "IRR"),
        )

        val problem = assertInvalid(result, TransactionField.CURRENCY, null)
        assertEquals(
            TransferProblem.CurrencyMismatch(sourceCurrency = "USD", destinationCurrency = "IRR"),
            problem,
        )
    }

    @Test
    fun `draft currency must match the source account`() {
        val result = TransferValidator.validate(draft(currencyCode = "IRR"), source, destination)

        val problem = assertInvalid(result, TransactionField.CURRENCY, null)
        assertEquals(
            TransferProblem.CurrencyMismatch(sourceCurrency = "USD", destinationCurrency = "IRR"),
            problem,
        )
    }

    @Test
    fun `multiple problems are reported together`() {
        val result = TransferValidator.validate(
            draft(amountMinor = -1L, toAccountId = "missing"),
            source,
            null,
        )

        val problems = (result as TransferValidation.Invalid).problems
        assertEquals(2, problems.size)
        assertTrue(problems.containsKey(TransactionField.AMOUNT))
        assertTrue(problems.containsKey(TransactionField.TO_ACCOUNT))
        assertFalse(problems.containsKey(TransactionField.CURRENCY))
    }

    private fun draft(
        fromAccountId: String = "cash",
        toAccountId: String = "card",
        amountMinor: Long = 125_000L,
        currencyCode: String = "USD",
    ) = TransferDraft(
        fromAccountId = fromAccountId,
        toAccountId = toAccountId,
        amountMinor = amountMinor,
        currencyCode = currencyCode,
        occurredAt = 1_700_000_000_000L,
    )

    private fun assertInvalid(
        result: TransferValidation,
        field: TransactionField,
        problem: TransferProblem?,
    ): TransferProblem? {
        assertTrue(result is TransferValidation.Invalid)
        val actual = (result as TransferValidation.Invalid).problems[field]
        if (problem != null) {
            assertEquals(problem, actual)
        }
        return actual
    }
}
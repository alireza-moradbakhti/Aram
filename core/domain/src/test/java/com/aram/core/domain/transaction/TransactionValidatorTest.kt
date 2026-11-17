package com.aram.core.domain.transaction

import com.aram.core.model.CategoryKind
import com.aram.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionValidatorTest {

    private fun invalidProblems(validation: TransactionValidation): Map<TransactionField, TransactionProblem> {
        assertTrue("expected invalid, got $validation", validation is TransactionValidation.Invalid)
        return (validation as TransactionValidation.Invalid).problems
    }

    @Test
    fun `valid expense passes`() {
        val validation = TransactionValidator.validate(
            draft = testDraft(),
            account = testAccount(),
            category = testCategory(),
        )

        assertEquals(TransactionValidation.Valid, validation)
    }

    @Test
    fun `valid income passes with an income category`() {
        val validation = TransactionValidator.validate(
            draft = testDraft(type = TransactionType.INCOME),
            account = testAccount(),
            category = testCategory(kind = CategoryKind.INCOME),
        )

        assertEquals(TransactionValidation.Valid, validation)
    }

    @Test
    fun `zero amount is rejected`() {
        val problems = invalidProblems(
            TransactionValidator.validate(
                draft = testDraft(amountMinor = 0L),
                account = testAccount(),
                category = testCategory(),
            ),
        )

        assertEquals(TransactionProblem.AmountNotPositive, problems[TransactionField.AMOUNT])
    }

    @Test
    fun `negative amount is rejected`() {
        val problems = invalidProblems(
            TransactionValidator.validate(
                draft = testDraft(amountMinor = -500L),
                account = testAccount(),
                category = testCategory(),
            ),
        )

        assertEquals(TransactionProblem.AmountNotPositive, problems[TransactionField.AMOUNT])
    }

    @Test
    fun `missing account is rejected`() {
        val problems = invalidProblems(
            TransactionValidator.validate(
                draft = testDraft(),
                account = null,
                category = testCategory(),
            ),
        )

        assertEquals(TransactionProblem.AccountNotFound, problems[TransactionField.ACCOUNT])
    }

    @Test
    fun `archived account is rejected`() {
        val problems = invalidProblems(
            TransactionValidator.validate(
                draft = testDraft(),
                account = testAccount(archived = true),
                category = testCategory(),
            ),
        )

        assertEquals(TransactionProblem.AccountArchived, problems[TransactionField.ACCOUNT])
    }

    @Test
    fun `currency mismatch is rejected`() {
        val problems = invalidProblems(
            TransactionValidator.validate(
                draft = testDraft(currencyCode = "USD"),
                account = testAccount(currencyCode = "IRR"),
                category = testCategory(),
            ),
        )

        assertEquals(
            TransactionProblem.CurrencyMismatch(accountCurrency = "IRR", draftCurrency = "USD"),
            problems[TransactionField.CURRENCY],
        )
    }

    @Test
    fun `expense without a category is rejected`() {
        val problems = invalidProblems(
            TransactionValidator.validate(
                draft = testDraft(categoryId = null),
                account = testAccount(),
                category = null,
            ),
        )

        assertEquals(TransactionProblem.CategoryRequired, problems[TransactionField.CATEGORY])
    }

    @Test
    fun `unknown category is rejected`() {
        val problems = invalidProblems(
            TransactionValidator.validate(
                draft = testDraft(),
                account = testAccount(),
                category = null,
            ),
        )

        assertEquals(TransactionProblem.CategoryNotFound, problems[TransactionField.CATEGORY])
    }

    @Test
    fun `archived category is rejected`() {
        val problems = invalidProblems(
            TransactionValidator.validate(
                draft = testDraft(),
                account = testAccount(),
                category = testCategory(archived = true),
            ),
        )

        assertEquals(TransactionProblem.CategoryArchived, problems[TransactionField.CATEGORY])
    }

    @Test
    fun `expense with an income category is rejected`() {
        val problems = invalidProblems(
            TransactionValidator.validate(
                draft = testDraft(type = TransactionType.EXPENSE),
                account = testAccount(),
                category = testCategory(kind = CategoryKind.INCOME),
            ),
        )

        assertEquals(
            TransactionProblem.CategoryKindMismatch(
                expected = CategoryKind.EXPENSE,
                actual = CategoryKind.INCOME,
            ),
            problems[TransactionField.CATEGORY],
        )
    }

    @Test
    fun `income with an expense category is rejected`() {
        val problems = invalidProblems(
            TransactionValidator.validate(
                draft = testDraft(type = TransactionType.INCOME),
                account = testAccount(),
                category = testCategory(kind = CategoryKind.EXPENSE),
            ),
        )

        assertEquals(
            TransactionProblem.CategoryKindMismatch(
                expected = CategoryKind.INCOME,
                actual = CategoryKind.EXPENSE,
            ),
            problems[TransactionField.CATEGORY],
        )
    }

    @Test
    fun `transfer without a category passes`() {
        val validation = TransactionValidator.validate(
            draft = testDraft(type = TransactionType.TRANSFER, categoryId = null),
            account = testAccount(),
            category = null,
        )

        assertEquals(TransactionValidation.Valid, validation)
    }

    @Test
    fun `transfer with a category is rejected`() {
        val problems = invalidProblems(
            TransactionValidator.validate(
                draft = testDraft(type = TransactionType.TRANSFER, categoryId = "cat"),
                account = testAccount(),
                category = testCategory(),
            ),
        )

        assertEquals(TransactionProblem.CategoryNotAllowed, problems[TransactionField.CATEGORY])
    }
}

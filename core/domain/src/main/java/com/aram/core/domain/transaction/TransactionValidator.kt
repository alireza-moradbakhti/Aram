package com.aram.core.domain.transaction

import com.aram.core.model.Account
import com.aram.core.model.Category
import com.aram.core.model.CategoryKind
import com.aram.core.model.TransactionType

/**
 * Pure ledger rules. Callers resolve the account and category first; a null
 * lookup means the id does not exist in the current ledger.
 */
object TransactionValidator {

    fun validate(
        draft: TransactionDraft,
        account: Account?,
        category: Category?,
    ): TransactionValidation {
        val problems = mutableMapOf<TransactionField, TransactionProblem>()

        if (draft.amountMinor <= 0L) {
            problems[TransactionField.AMOUNT] = TransactionProblem.AmountNotPositive
        }

        when {
            account == null ->
                problems[TransactionField.ACCOUNT] = TransactionProblem.AccountNotFound
            account.archived ->
                problems[TransactionField.ACCOUNT] = TransactionProblem.AccountArchived
            account.currencyCode != draft.currencyCode ->
                problems[TransactionField.CURRENCY] = TransactionProblem.CurrencyMismatch(
                    accountCurrency = account.currencyCode,
                    draftCurrency = draft.currencyCode,
                )
        }

        when (draft.type) {
            TransactionType.TRANSFER -> {
                if (draft.categoryId != null) {
                    problems[TransactionField.CATEGORY] = TransactionProblem.CategoryNotAllowed
                }
            }
            TransactionType.INCOME, TransactionType.EXPENSE -> {
                val expectedKind = draft.type.expectedCategoryKind()
                when {
                    draft.categoryId == null ->
                        problems[TransactionField.CATEGORY] = TransactionProblem.CategoryRequired
                    category == null ->
                        problems[TransactionField.CATEGORY] = TransactionProblem.CategoryNotFound
                    category.archived ->
                        problems[TransactionField.CATEGORY] = TransactionProblem.CategoryArchived
                    category.kind != expectedKind ->
                        problems[TransactionField.CATEGORY] = TransactionProblem.CategoryKindMismatch(
                            expected = expectedKind,
                            actual = category.kind,
                        )
                }
            }
        }

        return if (problems.isEmpty()) {
            TransactionValidation.Valid
        } else {
            TransactionValidation.Invalid(problems)
        }
    }

    private fun TransactionType.expectedCategoryKind(): CategoryKind = when (this) {
        TransactionType.INCOME -> CategoryKind.INCOME
        TransactionType.EXPENSE -> CategoryKind.EXPENSE
        TransactionType.TRANSFER ->
            error("Transfers never carry a category kind")
    }
}

package com.aram.core.domain.transaction

import com.aram.core.model.AppError
import com.aram.core.model.CategoryKind
import com.aram.core.model.Transaction

/** Fields a validation problem can attach to, so the UI can highlight them. */
enum class TransactionField {
    AMOUNT,
    ACCOUNT,
    CATEGORY,
    CURRENCY,
}

sealed interface TransactionProblem {
    data object AmountNotPositive : TransactionProblem

    data object AccountNotFound : TransactionProblem

    data object AccountArchived : TransactionProblem

    data object CategoryRequired : TransactionProblem

    data object CategoryNotFound : TransactionProblem

    data object CategoryArchived : TransactionProblem

    data class CategoryKindMismatch(
        val expected: CategoryKind,
        val actual: CategoryKind,
    ) : TransactionProblem

    data object CategoryNotAllowed : TransactionProblem

    data class CurrencyMismatch(
        val accountCurrency: String,
        val draftCurrency: String,
    ) : TransactionProblem
}

sealed interface TransactionValidation {
    data object Valid : TransactionValidation

    data class Invalid(
        val problems: Map<TransactionField, TransactionProblem>,
    ) : TransactionValidation
}

sealed interface TransactionWriteResult {
    data class Success(val transaction: Transaction) : TransactionWriteResult

    data class Invalid(
        val problems: Map<TransactionField, TransactionProblem>,
    ) : TransactionWriteResult

    data class Failed(val error: AppError) : TransactionWriteResult
}

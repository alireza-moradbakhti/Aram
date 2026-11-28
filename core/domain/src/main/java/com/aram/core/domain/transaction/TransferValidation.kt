package com.aram.core.domain.transaction

import com.aram.core.model.AppError

sealed interface TransferProblem {
    data object AmountNotPositive : TransferProblem

    data object SameAccount : TransferProblem

    data object AccountNotFound : TransferProblem

    data object AccountArchived : TransferProblem

    data class CurrencyMismatch(
        val sourceCurrency: String,
        val destinationCurrency: String,
    ) : TransferProblem
}

sealed interface TransferValidation {
    data object Valid : TransferValidation

    data class Invalid(
        val problems: Map<TransactionField, TransferProblem>,
    ) : TransferValidation
}

sealed interface TransferWriteResult {
    data class Success(val transferGroupId: String) : TransferWriteResult

    data class Invalid(
        val problems: Map<TransactionField, TransferProblem>,
    ) : TransferWriteResult

    data class Failed(val error: AppError) : TransferWriteResult
}
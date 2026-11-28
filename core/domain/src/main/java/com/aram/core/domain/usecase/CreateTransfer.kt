package com.aram.core.domain.usecase

import com.aram.core.domain.repository.LedgerLookup
import com.aram.core.domain.repository.TransactionRepository
import com.aram.core.domain.transaction.TransferDraft
import com.aram.core.domain.transaction.TransferValidation
import com.aram.core.domain.transaction.TransferValidator
import com.aram.core.domain.transaction.TransferWriteResult
import com.aram.core.model.Outcome

class CreateTransfer(
    private val repository: TransactionRepository,
    private val lookup: LedgerLookup,
) {

    suspend operator fun invoke(draft: TransferDraft): TransferWriteResult {
        val source = when (val result = lookup.account(draft.fromAccountId)) {
            is Outcome.Failure -> return TransferWriteResult.Failed(result.error)
            is Outcome.Success -> result.value
        }
        val destination = when (val result = lookup.account(draft.toAccountId)) {
            is Outcome.Failure -> return TransferWriteResult.Failed(result.error)
            is Outcome.Success -> result.value
        }

        when (val validation = TransferValidator.validate(draft, source, destination)) {
            TransferValidation.Valid -> Unit
            is TransferValidation.Invalid ->
                return TransferWriteResult.Invalid(validation.problems)
        }

        return when (val result = repository.createTransfer(draft)) {
            is Outcome.Success -> TransferWriteResult.Success(result.value)
            is Outcome.Failure -> TransferWriteResult.Failed(result.error)
        }
    }
}
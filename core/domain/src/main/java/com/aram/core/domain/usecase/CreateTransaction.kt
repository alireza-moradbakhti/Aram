package com.aram.core.domain.usecase

import com.aram.core.domain.repository.LedgerLookup
import com.aram.core.domain.repository.TransactionRepository
import com.aram.core.domain.transaction.TransactionDraft
import com.aram.core.domain.transaction.TransactionValidation
import com.aram.core.domain.transaction.TransactionValidator
import com.aram.core.domain.transaction.TransactionWriteResult
import com.aram.core.model.Outcome

class CreateTransaction(
    private val repository: TransactionRepository,
    private val lookup: LedgerLookup,
) {

    suspend operator fun invoke(draft: TransactionDraft): TransactionWriteResult {
        val account = when (val result = lookup.account(draft.accountId)) {
            is Outcome.Failure -> return TransactionWriteResult.Failed(result.error)
            is Outcome.Success -> result.value
        }
        val category = draft.categoryId?.let { id ->
            when (val result = lookup.category(id)) {
                is Outcome.Failure -> return TransactionWriteResult.Failed(result.error)
                is Outcome.Success -> result.value
            }
        }

        when (val validation = TransactionValidator.validate(draft, account, category)) {
            TransactionValidation.Valid -> Unit
            is TransactionValidation.Invalid ->
                return TransactionWriteResult.Invalid(validation.problems)
        }

        return when (val result = repository.create(draft)) {
            is Outcome.Success -> TransactionWriteResult.Success(result.value)
            is Outcome.Failure -> TransactionWriteResult.Failed(result.error)
        }
    }
}

package com.aram.core.domain.usecase

import com.aram.core.domain.repository.TransactionRepository
import com.aram.core.model.Outcome

class DeleteTransaction(
    private val repository: TransactionRepository,
) {

    suspend operator fun invoke(id: String): Outcome<Unit> = repository.softDelete(id)
}

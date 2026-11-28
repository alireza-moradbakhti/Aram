package com.aram.core.domain.usecase

import com.aram.core.domain.repository.TransactionRepository
import com.aram.core.model.Outcome

class RestoreTransaction(
    private val repository: TransactionRepository,
) {

    suspend operator fun invoke(id: String): Outcome<Unit> = repository.restore(id)
}
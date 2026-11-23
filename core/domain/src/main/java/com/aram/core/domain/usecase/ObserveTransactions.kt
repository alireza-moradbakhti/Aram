package com.aram.core.domain.usecase

import com.aram.core.domain.repository.TransactionRepository
import com.aram.core.model.Transaction
import kotlinx.coroutines.flow.Flow

class ObserveTransactions(
    private val repository: TransactionRepository,
) {

    operator fun invoke(): Flow<List<Transaction>> = repository.observeTransactions()
}

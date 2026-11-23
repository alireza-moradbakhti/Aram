package com.aram.core.domain.repository

import com.aram.core.model.Account
import com.aram.core.model.Category
import com.aram.core.model.Outcome

/**
 * Read-only account and category access for validation. Kept separate from
 * [TransactionRepository] so use cases resolve ownership without depending on
 * a concrete ledger repository.
 */
interface LedgerLookup {

    suspend fun account(id: String): Outcome<Account?>

    suspend fun category(id: String): Outcome<Category?>
}

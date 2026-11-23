package com.aram.core.domain.repository

import com.aram.core.model.Account
import com.aram.core.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * Live, non-archived accounts and categories for pickers. Kept apart from
 * [LedgerLookup] so use cases resolve single rows without depending on lists.
 */
interface LedgerDirectory {

    fun observeAccounts(): Flow<List<Account>>

    fun observeCategories(): Flow<List<Category>>
}

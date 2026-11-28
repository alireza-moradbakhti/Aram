package com.aram.core.domain.repository

import com.aram.core.model.AccountBalance
import kotlinx.coroutines.flow.Flow

/**
 * Derived, never-stored account balances. Emits null when the account is
 * unknown or the session is signed out.
 */
interface AccountBalanceRepository {

    fun observeBalance(accountId: String): Flow<AccountBalance?>
}
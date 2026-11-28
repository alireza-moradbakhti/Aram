package com.aram.core.data.repository

import com.aram.core.common.CoroutineDispatchers
import com.aram.core.data.FakeAccountDao
import com.aram.core.database.dao.AccountBalanceRow
import com.aram.core.datastore.SessionStore
import com.aram.core.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountBalanceRepositoryImplTest {

    private val accountDao = FakeAccountDao()
    private val session = BalanceFakeSessionStore()
    private val repository = AccountBalanceRepositoryImpl(
        accountDao = accountDao,
        session = session,
        dispatchers = BalanceTestDispatchers,
        logger = Logger(),
    )

    @Test
    fun `maps the dao row for the session user`() = runTest {
        accountDao.balance.value = AccountBalanceRow("acc", "IRR", 12_345L)

        val balance = repository.observeBalance("acc").first()

        assertEquals("acc", balance?.accountId)
        assertEquals("IRR", balance?.currencyCode)
        assertEquals(12_345L, balance?.balanceMinor)
        assertEquals("user-1", accountDao.lastBalanceUserId)
    }

    @Test
    fun `a missing account emits null`() = runTest {
        assertNull(repository.observeBalance("missing").first())
    }

    @Test
    fun `no session emits null without querying the dao`() = runTest {
        session.userId.value = null
        accountDao.balance.value = AccountBalanceRow("acc", "IRR", 99L)

        assertNull(repository.observeBalance("acc").first())
        assertNull(accountDao.lastBalanceUserId)
    }

    @Test
    fun `a dao failure emits null instead of crashing`() = runTest {
        accountDao.balanceError = IllegalStateException("boom")

        assertNull(repository.observeBalance("acc").first())
    }
}

private object BalanceTestDispatchers : CoroutineDispatchers {
    override val io = Dispatchers.Unconfined
    override val default = Dispatchers.Unconfined
    override val main = Dispatchers.Unconfined
}

private class BalanceFakeSessionStore : SessionStore {
    override val userId = MutableStateFlow<String?>("user-1")
    override suspend fun setUserId(userId: String) {
        this.userId.value = userId
    }

    override suspend fun clear() {
        userId.value = null
    }
}
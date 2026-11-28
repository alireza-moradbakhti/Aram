package com.aram.core.data.repository

import com.aram.core.common.CoroutineDispatchers
import com.aram.core.data.mapper.toModel
import com.aram.core.database.dao.AccountDao
import com.aram.core.datastore.SessionStore
import com.aram.core.domain.repository.AccountBalanceRepository
import com.aram.core.logging.Logger
import com.aram.core.model.AccountBalance
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@Singleton
class AccountBalanceRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val session: SessionStore,
    private val dispatchers: CoroutineDispatchers,
    private val logger: Logger,
) : AccountBalanceRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeBalance(accountId: String): Flow<AccountBalance?> =
        session.userId
            .flatMapLatest { userId ->
                if (userId == null) flowOf(null) else accountDao.observeBalance(userId, accountId)
            }
            .map { row -> row?.toModel() }
            .catch { e ->
                if (e is CancellationException) throw e
                logger.e(COMPONENT, "observeBalance failed", e)
                emit(null)
            }
            .flowOn(dispatchers.io)

    private companion object {
        const val COMPONENT = "AccountBalanceRepository"
    }
}
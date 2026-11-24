package com.aram.core.data.repository

import com.aram.core.common.CoroutineDispatchers
import com.aram.core.data.mapper.toEntity
import com.aram.core.database.dao.AccountDao
import com.aram.core.database.dao.CategoryDao
import com.aram.core.database.dao.TransactionDao
import com.aram.core.database.dao.UserDao
import com.aram.core.database.entity.TransactionEntity
import com.aram.core.datastore.SessionStore
import com.aram.core.datastore.SyncMetadata
import com.aram.core.datastore.SyncMetadataStore
import com.aram.core.domain.repository.LedgerSync
import com.aram.core.domain.repository.OutboxSync
import com.aram.core.logging.Logger
import com.aram.core.model.SyncState
import com.aram.core.model.SyncStatus
import com.aram.core.network.api.ApiError
import com.aram.core.network.api.ApiResult
import com.aram.core.network.api.safeApiCall
import com.aram.core.network.config.ApiPaths
import com.aram.core.network.config.EndpointProvider
import com.aram.core.network.dto.AccountDto
import com.aram.core.network.dto.CategoryRefDto
import com.aram.core.network.dto.SyncPullDto
import com.aram.core.network.dto.TransactionDto
import com.aram.core.network.dto.TransactionListDto
import com.aram.core.network.http.TokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.http.HttpMethod
import java.time.Clock
import java.time.YearMonth
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Cache-first ledger refresh: drains queued writes, pulls changes since the
 * stored cursor, then pulls one UTC month plus the category/account references
 * it needs. Merges into Room with last-write-wins and tombstones rows the
 * server no longer returns. The API hard-deletes, so a complete page sweep of
 * the month is the only tombstone signal.
 */
@Singleton
class LedgerSyncRepositoryImpl @Inject constructor(
    private val client: HttpClient,
    private val endpoints: EndpointProvider,
    private val session: SessionStore,
    private val tokenProvider: TokenProvider,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val userDao: UserDao,
    private val syncMetadata: SyncMetadataStore,
    private val outboxSync: OutboxSync,
    private val dispatchers: CoroutineDispatchers,
    private val logger: Logger,
    private val clock: Clock,
) : LedgerSync {

    private val _state = MutableStateFlow(SyncState())
    override val state: Flow<SyncState> = _state.asStateFlow()

    private val mutex = Mutex()

    override suspend fun refresh(month: YearMonth, force: Boolean) {
        runSync(month, force)
    }

    override suspend fun syncNow(): Boolean = runSync(YearMonth.now(clock), force = true)

    private suspend fun runSync(month: YearMonth, force: Boolean): Boolean = try {
        mutex.withLock { refreshLocked(month, force) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        logger.e(COMPONENT, "Ledger refresh crashed", e)
        _state.value = SyncState(SyncStatus.FAILED, syncMetadata.read()?.lastSyncedAtMillis)
        false
    }

    private suspend fun refreshLocked(month: YearMonth, force: Boolean): Boolean {
        val userId = session.userId.first() ?: return true
        if (tokenProvider.accessToken() == null) return true

        val stored = syncMetadata.read()
        val now = clock.millis()
        val recentlySynced = stored?.month == month.toString() &&
            now - stored.lastSyncedAtMillis in 0 until STALE_AFTER_MILLIS
        if (!force && recentlySynced) {
            _state.value = SyncState(SyncStatus.IDLE, stored.lastSyncedAtMillis)
            return true
        }

        _state.value = SyncState(SyncStatus.REFRESHING, stored?.lastSyncedAtMillis)
        outboxSync.flush()
        return when (val result = pull(userId, month, stored?.cursor)) {
            is ApiResult.Success -> {
                val stamp = clock.millis()
                syncMetadata.write(SyncMetadata(stamp, month.toString(), result.data))
                _state.value = SyncState(SyncStatus.IDLE, stamp)
                true
            }
            is ApiResult.Failure -> {
                logger.w(COMPONENT, "Ledger refresh failed: ${result.error::class.simpleName}")
                _state.value = SyncState(statusFor(result.error), stored?.lastSyncedAtMillis)
                false
            }
        }
    }

    private fun statusFor(error: ApiError): SyncStatus = when (error) {
        ApiError.Offline, ApiError.Timeout -> SyncStatus.OFFLINE
        ApiError.Unauthorized -> SyncStatus.IDLE
        is ApiError.Server -> SyncStatus.FAILED
        else -> SyncStatus.STALE
    }

    private suspend fun pull(
        userId: String,
        month: YearMonth,
        cursor: String?,
    ): ApiResult<String?> =
        withContext(dispatchers.io) {
            val startedAt = clock.millis()

            val categories = when (
                val result = safeApiCall<List<CategoryRefDto>>(
                    client = client,
                    logger = logger,
                    method = HttpMethod.Get,
                    url = endpoints.url(ApiPaths.CATEGORIES),
                )
            ) {
                is ApiResult.Failure -> return@withContext result
                is ApiResult.Success -> result.data
            }

            val accounts = when (
                val result = safeApiCall<List<AccountDto>>(
                    client = client,
                    logger = logger,
                    method = HttpMethod.Get,
                    url = endpoints.url(ApiPaths.PROFILE_ACCOUNTS),
                )
            ) {
                is ApiResult.Failure -> return@withContext result
                is ApiResult.Success -> result.data
            }

            val delta = when (
                val result = safeApiCall<SyncPullDto>(
                    client = client,
                    logger = logger,
                    method = HttpMethod.Get,
                    url = endpoints.url(ApiPaths.SYNC_PULL),
                    configure = { if (cursor != null) parameter("since", cursor) },
                )
            ) {
                is ApiResult.Failure -> return@withContext result
                is ApiResult.Success -> result.data
            }
            upsertPreservingLocal(
                userId = userId,
                incoming = delta.transactions.mapNotNull { dto -> dto.toEntity(userId, startedAt) },
                startedAt = startedAt,
            )
            // The API caps a pull at 500 rows with no paging, so a full page is
            // treated as partial and the old cursor stays put.
            val nextCursor = if (delta.transactions.size < CURSOR_PULL_CAP) delta.serverTime else cursor

            val rows = mutableListOf<TransactionDto>()
            var page = 1
            var totalPages = 1
            while (page <= totalPages && page <= MAX_PAGES) {
                val result = safeApiCall<TransactionListDto>(
                    client = client,
                    logger = logger,
                    method = HttpMethod.Get,
                    url = endpoints.url(ApiPaths.TRANSACTIONS),
                    configure = {
                        parameter("year", month.year)
                        parameter("month", month.monthValue)
                        parameter("page", page)
                        parameter("pageSize", PAGE_SIZE)
                    },
                )
                val data = when (result) {
                    is ApiResult.Failure -> return@withContext result
                    is ApiResult.Success -> result.data
                }
                rows += data.groups.flatMap { it.items }
                totalPages = data.totalPages
                page++
            }

            applyPull(
                userId = userId,
                month = month,
                categories = categories,
                accounts = accounts,
                rows = rows,
                complete = page > totalPages,
                startedAt = startedAt,
            )
            ApiResult.Success(nextCursor)
        }

    private suspend fun applyPull(
        userId: String,
        month: YearMonth,
        categories: List<CategoryRefDto>,
        accounts: List<AccountDto>,
        rows: List<TransactionDto>,
        complete: Boolean,
        startedAt: Long,
    ) {
        val currency = userDao.getById(userId)?.defaultCurrency ?: DEFAULT_CURRENCY

        categoryDao.upsertAll(
            categories.mapIndexed { index, dto -> dto.toEntity(userId, startedAt, index) },
        )
        accountDao.upsertAll(
            accounts.mapIndexed { index, dto -> dto.toEntity(userId, currency, startedAt, index) },
        )
        if (categories.isNotEmpty()) {
            categoryDao.softDeleteOrphans(userId, categories.map { it.id }, startedAt)
        }
        if (accounts.isNotEmpty()) {
            accountDao.softDeleteOrphans(userId, accounts.map { it.id }, startedAt)
        }

        val incoming = rows.mapNotNull { dto -> dto.toEntity(userId, startedAt) }
        upsertPreservingLocal(userId, incoming, startedAt)

        if (!complete) return

        val (fromInclusive, toExclusive) = utcMonthWindow(month)
        if (incoming.isEmpty()) {
            transactionDao.softDeleteAllInRange(userId, fromInclusive, toExclusive, startedAt)
        } else {
            transactionDao.softDeleteMissing(
                userId = userId,
                fromInclusive = fromInclusive,
                toExclusive = toExclusive,
                keepIds = incoming.map { it.id },
                deletedAt = startedAt,
            )
        }
    }

    /** An unflushed or just-written local row wins over the pulled copy. */
    private suspend fun upsertPreservingLocal(
        userId: String,
        incoming: List<TransactionEntity>,
        startedAt: Long,
    ) {
        if (incoming.isEmpty()) return
        val existing = transactionDao
            .findByIds(userId, incoming.map { it.id })
            .associateBy { it.id }
        val localWins = existing.values
            .filter { it.pendingPush || it.updatedAt > startedAt }
            .map { it.id }
            .toSet()
        transactionDao.upsertAll(incoming.filter { it.id !in localWins })
    }

    private fun utcMonthWindow(month: YearMonth): Pair<Long, Long> {
        val start = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1)
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        return start to end
    }

    private companion object {
        const val COMPONENT = "LedgerSync"
        const val PAGE_SIZE = 100
        const val MAX_PAGES = 5
        const val CURSOR_PULL_CAP = 500
        const val STALE_AFTER_MILLIS = 15L * 60L * 1000L
        const val DEFAULT_CURRENCY = "IRR"
    }
}

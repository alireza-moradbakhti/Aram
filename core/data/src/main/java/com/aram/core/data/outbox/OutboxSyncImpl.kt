package com.aram.core.data.outbox

import com.aram.core.common.CoroutineDispatchers
import com.aram.core.database.dao.SyncOutboxDao
import com.aram.core.database.entity.SyncOutboxEntity
import com.aram.core.datastore.SessionStore
import com.aram.core.domain.repository.OutboxSnapshot
import com.aram.core.domain.repository.OutboxSync
import com.aram.core.logging.Logger
import com.aram.core.network.IDEMPOTENCY_KEY
import com.aram.core.network.api.ApiError
import com.aram.core.network.api.ApiResult
import com.aram.core.network.api.safeApiCall
import com.aram.core.network.config.ApiPaths
import com.aram.core.network.config.EndpointProvider
import com.aram.core.network.dto.SyncPushRequest
import com.aram.core.network.dto.SyncPushResultDto
import com.aram.core.network.dto.SyncTransactionItem
import com.aram.core.network.http.TokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Drains [SyncOutboxEntity] rows oldest-first. Each entry keeps its id as the
 * idempotency key, so a retried push is safe. A transient failure stops the
 * run and reschedules; a rejected entry parks until the user retries or
 * discards it.
 */
@Singleton
class OutboxSyncImpl @Inject constructor(
    private val client: HttpClient,
    private val endpoints: EndpointProvider,
    private val session: SessionStore,
    private val tokenProvider: TokenProvider,
    private val outboxDao: SyncOutboxDao,
    private val dispatchers: CoroutineDispatchers,
    private val logger: Logger,
    private val clock: Clock,
) : OutboxSync {

    private val mutex = Mutex()

    override val snapshot: Flow<OutboxSnapshot> = session.userId
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(OutboxSnapshot())
            } else {
                combine(
                    outboxDao.observeCount(userId),
                    outboxDao.observeFailed(userId, MAX_ATTEMPTS),
                ) { pending, failed -> OutboxSnapshot(pending = pending, failed = failed) }
            }
        }

    override suspend fun flush(): Boolean = try {
        mutex.withLock { flushLocked() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        logger.e(COMPONENT, "Outbox flush crashed", e)
        false
    }

    override suspend fun retryFailed() {
        val userId = session.userId.first() ?: return
        outboxDao.retryAll(userId)
    }

    override suspend fun discard(entityId: String) {
        val userId = session.userId.first() ?: return
        outboxDao.discard(userId, entityId)
    }

    private suspend fun flushLocked(): Boolean {
        val userId = session.userId.first() ?: return true
        // A local profile has a user but no token; entries wait for sign-in.
        // Returning false here would make the worker retry forever.
        if (tokenProvider.accessToken() == null) return true

        while (true) {
            val now = clock.millis()
            val due = withContext(dispatchers.io) { outboxDao.due(userId, now, BATCH_SIZE) }
            if (due.isEmpty()) return true

            for (entry in due) {
                when (val outcome = process(entry)) {
                    Outcome.Applied -> {
                        outboxDao.delete(entry.id)
                        outboxDao.markSynced(userId, entry.entityId)
                    }
                    is Outcome.Parked ->
                        outboxDao.markAttempt(entry.id, MAX_ATTEMPTS, PARKED_AT, outcome.reason)
                    is Outcome.RetryLater -> {
                        outboxDao.markAttempt(
                            id = entry.id,
                            attempts = entry.attempts + 1,
                            nextAttemptAt = now + backoffMillis(entry.attempts),
                            lastError = outcome.reason,
                        )
                        return false
                    }
                    Outcome.Stop -> return false
                }
            }
        }
    }

    private suspend fun process(entry: SyncOutboxEntity): Outcome = when (entry.op) {
        SyncOutboxEntity.OP_DELETE -> deleteRemote(entry)
        else -> pushItem(entry)
    }

    private suspend fun pushItem(entry: SyncOutboxEntity): Outcome {
        val item = entry.payload?.let {
            runCatching { com.aram.core.network.AramJson.decodeFromString<SyncTransactionItem>(it) }
                .getOrNull()
        } ?: return Outcome.Parked("Missing payload")

        val result = safeApiCall<SyncPushResultDto>(
            client = client,
            logger = logger,
            method = HttpMethod.Post,
            url = endpoints.url(ApiPaths.SYNC_PUSH),
            configure = {
                header(IDEMPOTENCY_KEY, entry.id)
                contentType(ContentType.Application.Json)
                setBody(SyncPushRequest(items = listOf(item)))
            },
        )
        return when (result) {
            is ApiResult.Success -> Outcome.Applied
            is ApiResult.Failure -> result.error.toOutcome()
        }
    }

    private suspend fun deleteRemote(entry: SyncOutboxEntity): Outcome = when (
        val result = safeApiCall<Boolean>(
            client = client,
            logger = logger,
            method = HttpMethod.Delete,
            url = endpoints.url(ApiPaths.byId(ApiPaths.TRANSACTIONS, entry.entityId)),
            configure = { header(IDEMPOTENCY_KEY, entry.id) },
        )
    ) {
        is ApiResult.Success -> Outcome.Applied
        is ApiResult.Failure ->
            if (result.error == ApiError.NotFound) Outcome.Applied else result.error.toOutcome()
    }

    private fun ApiError.toOutcome(): Outcome = when (this) {
        ApiError.Unauthorized -> Outcome.Stop
        ApiError.Forbidden, ApiError.NotFound -> Outcome.Parked(this::class.simpleName ?: "Rejected")
        is ApiError.Conflict -> Outcome.Applied
        is ApiError.Validation -> Outcome.Parked(message ?: "Validation failed")
        ApiError.Offline, ApiError.Timeout, is ApiError.Server, is ApiError.RateLimited,
        is ApiError.Unknown,
        -> Outcome.RetryLater(this::class.simpleName ?: "Retry")
    }

    private fun backoffMillis(attempts: Int): Long =
        min(INITIAL_BACKOFF_MILLIS shl attempts.coerceAtMost(7), MAX_BACKOFF_MILLIS)

    private sealed interface Outcome {
        data object Applied : Outcome
        data object Stop : Outcome
        data class Parked(val reason: String) : Outcome
        data class RetryLater(val reason: String) : Outcome
    }

    private companion object {
        const val COMPONENT = "OutboxSync"
        const val BATCH_SIZE = 25
        const val MAX_ATTEMPTS = 6
        const val PARKED_AT = Long.MAX_VALUE / 2
        const val INITIAL_BACKOFF_MILLIS = 30_000L
        const val MAX_BACKOFF_MILLIS = 60L * 60L * 1000L
    }
}

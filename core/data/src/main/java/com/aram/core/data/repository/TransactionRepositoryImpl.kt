package com.aram.core.data.repository

import android.database.sqlite.SQLiteException
import com.aram.core.common.CoroutineDispatchers
import com.aram.core.common.error.safeOutcome
import com.aram.core.common.error.toAppError
import com.aram.core.data.mapper.toModel
import com.aram.core.database.dao.AccountDao
import com.aram.core.database.dao.CategoryDao
import com.aram.core.database.dao.LedgerWriteDao
import com.aram.core.database.dao.TransactionDao
import com.aram.core.database.entity.AccountEntity
import com.aram.core.database.entity.CategoryEntity
import com.aram.core.database.entity.SyncOutboxEntity
import com.aram.core.database.entity.TransactionEntity
import com.aram.core.data.outbox.categorySlug
import com.aram.core.data.outbox.outboxEntry
import com.aram.core.datastore.SessionStore
import com.aram.core.domain.repository.LedgerDirectory
import com.aram.core.domain.repository.LedgerLookup
import com.aram.core.domain.repository.OutboxTrigger
import com.aram.core.domain.repository.TransactionRepository
import com.aram.core.domain.transaction.TransactionDraft
import com.aram.core.domain.transaction.TransactionFilter
import com.aram.core.logging.Logger
import com.aram.core.model.Account
import com.aram.core.model.AppError
import com.aram.core.model.Category
import com.aram.core.model.Outcome
import com.aram.core.model.Transaction
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val writeDao: LedgerWriteDao,
    private val outboxTrigger: OutboxTrigger,
    private val session: SessionStore,
    private val dispatchers: CoroutineDispatchers,
    private val logger: Logger,
) : TransactionRepository, LedgerLookup, LedgerDirectory {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeTransactions(
        fromInclusive: Long,
        toExclusive: Long,
    ): Flow<List<Transaction>> = observeLedger { userId ->
        transactionDao.observeRange(userId, fromInclusive, toExclusive)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeRecent(limit: Int): Flow<List<Transaction>> = observeLedger { userId ->
        transactionDao.observeRecent(userId, limit)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun search(filter: TransactionFilter): Flow<List<Transaction>> = observeLedger { userId ->
        transactionDao.search(
            userId = userId,
            noteQuery = filter.query.trim(),
            type = filter.type?.name,
            excludeType = filter.excludeType?.name,
            categoryId = filter.categoryId,
            fromInclusive = filter.fromInclusive,
            toExclusive = filter.toExclusive,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeTransaction(id: String): Flow<Transaction?> =
        session.userId
            .flatMapLatest { userId ->
                if (userId == null) flowOf(null) else transactionDao.observeById(userId, id)
            }
            .map { entity -> entity?.toModel() }
            .catch { e ->
                if (e is CancellationException) throw e
                logger.e(COMPONENT, "observeTransaction failed", e)
                emit(null)
            }
            .flowOn(dispatchers.io)

    override suspend fun create(draft: TransactionDraft): Outcome<Transaction> {
        val userId = when (val result = currentUserId()) {
            is Outcome.Success -> result.value
            is Outcome.Failure -> return result
        }
        return safeOutcome(
            component = COMPONENT,
            operation = "create",
            logger = logger,
            mapError = { it.toStorageAwareError() },
        ) {
            withContext(dispatchers.io) {
                val now = System.currentTimeMillis()
                val entity = TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    type = draft.type.name,
                    accountId = draft.accountId,
                    categoryId = draft.categoryId,
                    amountMinor = draft.amountMinor,
                    currencyCode = draft.currencyCode,
                    occurredAt = draft.occurredAt,
                    note = draft.note.cleanedNote(),
                    transferGroupId = null,
                    recurringRuleId = null,
                    createdAt = now,
                    updatedAt = now,
                    deletedAt = null,
                    pendingPush = true,
                )
                writeDao.createWithOutbox(
                    transaction = entity,
                    entry = outboxEntry(
                        userId = userId,
                        entity = entity,
                        op = SyncOutboxEntity.OP_CREATE,
                        categorySlug = categorySlug(
                            draft.categoryId,
                            draft.categoryId?.let { categoryDao.findById(userId, it) },
                        ),
                        now = now,
                    ),
                )
                outboxTrigger.schedule()
                entity.toModel()
            }
        }
    }

    override suspend fun update(id: String, draft: TransactionDraft): Outcome<Transaction> {
        val userId = when (val result = currentUserId()) {
            is Outcome.Success -> result.value
            is Outcome.Failure -> return result
        }
        return safeOutcome(
            component = COMPONENT,
            operation = "update",
            logger = logger,
            mapError = { it.toStorageAwareError() },
        ) {
            withContext(dispatchers.io) {
                val now = System.currentTimeMillis()
                val existing = transactionDao.findById(userId, id) ?: throw RowNotFound()
                val updated = existing.copy(
                    accountId = draft.accountId,
                    categoryId = draft.categoryId,
                    type = draft.type.name,
                    amountMinor = draft.amountMinor,
                    currencyCode = draft.currencyCode,
                    occurredAt = draft.occurredAt,
                    note = draft.note.cleanedNote(),
                    updatedAt = now,
                    pendingPush = true,
                )
                val changed = writeDao.updateWithOutbox(
                    userId = userId,
                    id = id,
                    accountId = draft.accountId,
                    categoryId = draft.categoryId,
                    type = draft.type.name,
                    amountMinor = draft.amountMinor,
                    currencyCode = draft.currencyCode,
                    occurredAt = draft.occurredAt,
                    note = draft.note.cleanedNote(),
                    updatedAt = now,
                    entry = outboxEntry(
                        userId = userId,
                        entity = updated,
                        op = SyncOutboxEntity.OP_UPDATE,
                        categorySlug = categorySlug(
                            draft.categoryId,
                            draft.categoryId?.let { categoryDao.findById(userId, it) },
                        ),
                        now = now,
                    ),
                )
                if (changed == 0) {
                    throw RowNotFound()
                }
                outboxTrigger.schedule()
                updated.toModel()
            }
        }
    }

    override suspend fun softDelete(id: String): Outcome<Unit> {
        val userId = when (val result = currentUserId()) {
            is Outcome.Success -> result.value
            is Outcome.Failure -> return result
        }
        return safeOutcome(
            component = COMPONENT,
            operation = "softDelete",
            logger = logger,
            mapError = { it.toStorageAwareError() },
        ) {
            withContext(dispatchers.io) {
                val now = System.currentTimeMillis()
                val existing = transactionDao.findById(userId, id) ?: throw RowNotFound()
                val changed = writeDao.deleteWithOutbox(
                    userId = userId,
                    id = id,
                    deletedAt = now,
                    entry = outboxEntry(
                        userId = userId,
                        entity = existing.copy(deletedAt = now, updatedAt = now),
                        op = SyncOutboxEntity.OP_DELETE,
                        categorySlug = null,
                        now = now,
                    ),
                )
                if (changed == 0) {
                    throw RowNotFound()
                }
                outboxTrigger.schedule()
            }
        }
    }

    override suspend fun restore(id: String): Outcome<Unit> {
        val userId = when (val result = currentUserId()) {
            is Outcome.Success -> result.value
            is Outcome.Failure -> return result
        }
        return safeOutcome(
            component = COMPONENT,
            operation = "restore",
            logger = logger,
            mapError = { it.toStorageAwareError() },
        ) {
            withContext(dispatchers.io) {
                val now = System.currentTimeMillis()
                val existing = transactionDao.findByIdIncludingDeleted(userId, id)
                    ?: throw RowNotFound()
                if (existing.deletedAt == null) return@withContext
                val restored = existing.copy(deletedAt = null, updatedAt = now, pendingPush = true)
                val changed = writeDao.restoreWithOutbox(
                    userId = userId,
                    id = id,
                    updatedAt = now,
                    entry = outboxEntry(
                        userId = userId,
                        entity = restored,
                        op = SyncOutboxEntity.OP_UPDATE,
                        categorySlug = categorySlug(
                            existing.categoryId,
                            existing.categoryId?.let { categoryDao.findById(userId, it) },
                        ),
                        now = now,
                    ),
                )
                if (changed == 0) {
                    throw RowNotFound()
                }
                outboxTrigger.schedule()
            }
        }
    }

    override suspend fun account(id: String): Outcome<Account?> {
        val userId = when (val result = currentUserId()) {
            is Outcome.Success -> result.value
            is Outcome.Failure -> return result
        }
        return safeOutcome(
            component = COMPONENT,
            operation = "account",
            logger = logger,
            mapError = { it.toStorageAwareError() },
        ) {
            withContext(dispatchers.io) {
                accountDao.findById(userId, id)?.toModel()
            }
        }
    }

    override suspend fun category(id: String): Outcome<Category?> {
        val userId = when (val result = currentUserId()) {
            is Outcome.Success -> result.value
            is Outcome.Failure -> return result
        }
        return safeOutcome(
            component = COMPONENT,
            operation = "category",
            logger = logger,
            mapError = { it.toStorageAwareError() },
        ) {
            withContext(dispatchers.io) {
                categoryDao.findById(userId, id)?.toModel()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAccounts(): Flow<List<Account>> = observeDirectory(
        name = "observeAccounts",
        query = accountDao::observeByUser,
    ) { rows -> rows.map(AccountEntity::toModel).filterNot { it.archived } }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCategories(): Flow<List<Category>> = observeDirectory(
        name = "observeCategories",
        query = categoryDao::observeByUser,
    ) { rows -> rows.map(CategoryEntity::toModel).filterNot { it.archived } }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T, R> observeDirectory(
        name: String,
        query: (userId: String) -> Flow<List<T>>,
        mapper: (List<T>) -> List<R>,
    ): Flow<List<R>> = session.userId
        .flatMapLatest { userId ->
            if (userId == null) flowOf(emptyList()) else query(userId)
        }
        .map(mapper)
        .catch { e ->
            if (e is CancellationException) throw e
            logger.e(COMPONENT, "$name failed", e)
            emit(emptyList())
        }
        .flowOn(dispatchers.io)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeLedger(
        query: (userId: String) -> Flow<List<TransactionEntity>>,
    ): Flow<List<Transaction>> =
        session.userId
            .flatMapLatest { userId ->
                if (userId == null) flowOf(emptyList()) else query(userId)
            }
            .map { entities -> entities.map(TransactionEntity::toModel) }
            .catch { e ->
                if (e is CancellationException) throw e
                logger.e(COMPONENT, "observeTransactions failed", e)
                emit(emptyList())
            }
            .flowOn(dispatchers.io)

    private suspend fun currentUserId(): Outcome<String> = try {
        val userId = session.userId.first()
        if (userId == null) {
            Outcome.Failure(AppError.Unauthorized)
        } else {
            Outcome.Success(userId)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        logger.e(COMPONENT, "session read failed", e)
        Outcome.Failure(AppError.Storage(e))
    }

    private fun Throwable.toStorageAwareError(): AppError = when (this) {
        is RowNotFound -> AppError.NotFound
        is SQLiteException -> AppError.Storage(this)
        else -> toAppError()
    }

    private fun String?.cleanedNote(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private class RowNotFound : IllegalStateException("Transaction row not found")

    private companion object {
        const val COMPONENT = "TransactionRepository"
    }
}

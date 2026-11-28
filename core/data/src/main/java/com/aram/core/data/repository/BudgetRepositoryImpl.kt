package com.aram.core.data.repository

import com.aram.core.common.CoroutineDispatchers
import com.aram.core.common.error.safeOutcome
import com.aram.core.common.error.toAppError
import com.aram.core.data.mapper.toModel
import com.aram.core.database.dao.BudgetDao
import com.aram.core.database.dao.CategoryDao
import com.aram.core.database.dao.TransactionDao
import com.aram.core.database.entity.BudgetEntity
import com.aram.core.database.entity.CategoryEntity
import com.aram.core.database.entity.TransactionEntity
import com.aram.core.datastore.SessionStore
import com.aram.core.domain.budget.BudgetDraft
import com.aram.core.domain.budget.BudgetProgress
import com.aram.core.domain.repository.BudgetRepository
import com.aram.core.logging.Logger
import com.aram.core.model.AppError
import com.aram.core.model.Budget
import com.aram.core.model.CategoryKind
import com.aram.core.model.Outcome
import com.aram.core.model.TransactionType
import com.aram.core.money.CurrencyCode
import java.time.Clock
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val session: SessionStore,
    private val clock: Clock,
    private val dispatchers: CoroutineDispatchers,
    private val logger: Logger,
) : BudgetRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeProgress(month: YearMonth): Flow<List<BudgetProgress>> {
        val periodStart = month.atDay(1)
        val fromInclusive = periodStart.atStartOfDay(clock.zone).toInstant().toEpochMilli()
        val toExclusive = periodStart.plusMonths(1).atStartOfDay(clock.zone).toInstant().toEpochMilli()
        return session.userId
            .flatMapLatest { userId ->
                if (userId == null) {
                    flowOf(emptyList())
                } else {
                    combine(
                        budgetDao.observeForPeriod(userId, periodStart.toEpochDay()),
                        categoryDao.observeByUser(userId),
                        transactionDao.observeRange(userId, fromInclusive, toExclusive),
                    ) { budgets, categories, rows ->
                        progress(budgets, categories, rows)
                    }
                }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                logger.e(COMPONENT, "observeProgress failed", e)
                emit(emptyList())
            }
            .flowOn(dispatchers.io)
    }

    private fun progress(
        budgets: List<BudgetEntity>,
        categories: List<CategoryEntity>,
        rows: List<TransactionEntity>,
    ): List<BudgetProgress> {
        val categoryById = categories.associateBy { it.id }
        val spentByCategoryAndCurrency = rows
            .asSequence()
            .filter { it.type == TransactionType.EXPENSE.name && it.deletedAt == null }
            .filter { it.categoryId != null }
            .groupBy { it.categoryId to it.currencyCode }
            .mapValues { (_, group) -> group.sumOf { it.amountMinor } }
        return budgets
            .mapNotNull { budget ->
                val category = categoryById[budget.categoryId]
                // Archived categories stay archived: their budget is hidden but kept.
                if (category == null || category.archived) {
                    null
                } else {
                    BudgetProgress(
                        budget = budget.toModel(),
                        category = category.toModel(),
                        spentMinor = spentByCategoryAndCurrency[budget.categoryId to budget.currencyCode] ?: 0L,
                    )
                }
            }
            .sortedWith(compareBy({ it.category.name }, { it.category.id }))
    }

    override suspend fun upsert(draft: BudgetDraft): Outcome<Budget> {
        val invalid = validate(draft)
        if (invalid != null) return Outcome.Failure(invalid)
        val userId = when (val result = currentUserId()) {
            is Outcome.Success -> result.value
            is Outcome.Failure -> return result
        }
        return safeOutcome(
            component = COMPONENT,
            operation = "upsert",
            logger = logger,
            mapError = { it.toStorageAwareError() },
        ) {
            withContext(dispatchers.io) {
                val category = categoryDao.findById(userId, draft.categoryId) ?: throw RowNotFound()
                if (category.kind != CategoryKind.EXPENSE.name || category.archived) {
                    throw InvalidCategory()
                }
                val currency = CurrencyCode.fromIsoCode(draft.currencyCode)
                // Wall-clock time, not the injected clock: timestamps are audit data.
                val now = System.currentTimeMillis()
                val periodStartEpochDay = draft.month.atDay(1).toEpochDay()
                val existing = budgetDao.find(userId, draft.categoryId, periodStartEpochDay)
                val entity = BudgetEntity(
                    id = existing?.id ?: stableId(userId, draft.categoryId, periodStartEpochDay),
                    userId = userId,
                    categoryId = draft.categoryId,
                    periodStartEpochDay = periodStartEpochDay,
                    limitMinor = draft.limitMinor,
                    currencyCode = currency.isoCode,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                    deletedAt = null,
                )
                budgetDao.upsert(entity)
                entity.toModel()
            }
        }
    }

    override suspend fun delete(id: String): Outcome<Unit> {
        val userId = when (val result = currentUserId()) {
            is Outcome.Success -> result.value
            is Outcome.Failure -> return result
        }
        return safeOutcome(
            component = COMPONENT,
            operation = "delete",
            logger = logger,
            mapError = { it.toStorageAwareError() },
        ) {
            withContext(dispatchers.io) {
                val changed = budgetDao.softDelete(userId, id, System.currentTimeMillis())
                if (changed == 0) throw RowNotFound()
            }
        }
    }

    private fun validate(draft: BudgetDraft): AppError? = when {
        draft.limitMinor <= 0L ->
            AppError.Validation(mapOf("limit" to "Enter an amount above zero."))

        runCatching { CurrencyCode.fromIsoCode(draft.currencyCode) }.isFailure ->
            AppError.Validation(mapOf("currency" to "Unsupported currency."))

        else -> null
    }

    private suspend fun currentUserId(): Outcome<String> = try {
        val userId = session.userId.first()
        if (userId == null) Outcome.Failure(AppError.Unauthorized) else Outcome.Success(userId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        logger.e(COMPONENT, "session read failed", e)
        Outcome.Failure(AppError.Storage(e))
    }

    private fun Throwable.toStorageAwareError(): AppError = when (this) {
        is RowNotFound -> AppError.NotFound
        is InvalidCategory -> AppError.Validation(mapOf("categoryId" to "Pick an expense category."))
        else -> toAppError()
    }

    private class RowNotFound : IllegalStateException("Budget category or row not found")

    private class InvalidCategory : IllegalStateException("Category cannot hold a budget")

    private companion object {
        const val COMPONENT = "BudgetRepository"

        fun stableId(userId: String, categoryId: String, periodStartEpochDay: Long): String =
            UUID.nameUUIDFromBytes(
                "budget:$userId:$categoryId:$periodStartEpochDay".toByteArray(Charsets.UTF_8),
            ).toString()
    }
}
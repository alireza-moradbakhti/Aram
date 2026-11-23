package com.aram.core.data

import com.aram.core.database.dao.AccountDao
import com.aram.core.database.dao.BudgetDao
import com.aram.core.database.dao.CategoryDao
import com.aram.core.database.dao.RecurringRuleDao
import com.aram.core.database.dao.SavingsGoalDao
import com.aram.core.database.dao.TransactionDao
import com.aram.core.database.dao.UserDao
import com.aram.core.database.entity.AccountEntity
import com.aram.core.database.entity.BudgetEntity
import com.aram.core.database.entity.CategoryEntity
import com.aram.core.database.entity.GoalContributionEntity
import com.aram.core.database.entity.RecurringRuleEntity
import com.aram.core.database.entity.SavingsGoalEntity
import com.aram.core.database.entity.TransactionEntity
import com.aram.core.database.entity.UserEntity
import com.aram.core.datastore.SessionStore
import com.aram.core.datastore.SessionToken
import com.aram.core.datastore.SessionTokenStore
import com.aram.core.datastore.SyncMetadata
import com.aram.core.datastore.SyncMetadataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeUserDao : UserDao {

    val rows = linkedMapOf<String, UserEntity>()
    private val flows = mutableMapOf<String, MutableStateFlow<UserEntity?>>()

    override suspend fun upsert(user: UserEntity) {
        rows[user.id] = user
        flowFor(user.id).value = user
    }

    override fun observeById(id: String): Flow<UserEntity?> = flowFor(id)

    override suspend fun getById(id: String): UserEntity? = rows[id]

    override suspend fun deleteById(id: String) {
        rows.remove(id)
        flowFor(id).value = null
    }

    private fun flowFor(id: String) = flows.getOrPut(id) { MutableStateFlow(rows[id]) }
}

class FakeCategoryDao : CategoryDao {

    val rows = linkedMapOf<String, CategoryEntity>()
    var upsertBatches = 0

    override suspend fun upsertAll(categories: List<CategoryEntity>) {
        upsertBatches++
        categories.forEach { rows[it.id] = it }
    }

    override fun observeByUser(userId: String): Flow<List<CategoryEntity>> =
        MutableStateFlow(rows.values.filter { it.userId == userId && it.deletedAt == null })

    override suspend fun findById(userId: String, id: String): CategoryEntity? =
        rows[id]?.takeIf { it.userId == userId && it.deletedAt == null }

    override suspend fun countByUser(userId: String): Int =
        rows.values.count { it.userId == userId && it.deletedAt == null }

    override suspend fun deleteByUser(userId: String) {
        rows.values.removeAll { it.userId == userId }
    }

    override suspend fun softDeleteOrphans(userId: String, keepIds: List<String>, deletedAt: Long): Int {
        var changed = 0
        rows.replaceAll { id, row ->
            if (row.userId == userId && row.deletedAt == null && id !in keepIds) {
                changed++
                row.copy(deletedAt = deletedAt, updatedAt = deletedAt)
            } else {
                row
            }
        }
        return changed
    }
}

class FakeAccountDao : AccountDao {

    val rows = linkedMapOf<String, AccountEntity>()

    override suspend fun upsertAll(accounts: List<AccountEntity>) {
        accounts.forEach { rows[it.id] = it }
    }

    override fun observeByUser(userId: String): Flow<List<AccountEntity>> =
        MutableStateFlow(rows.values.filter { it.userId == userId && it.deletedAt == null })

    override suspend fun findById(userId: String, id: String): AccountEntity? =
        rows[id]?.takeIf { it.userId == userId && it.deletedAt == null }

    override suspend fun countByUser(userId: String): Int =
        rows.values.count { it.userId == userId && it.deletedAt == null }

    override suspend fun deleteByUser(userId: String) {
        rows.values.removeAll { it.userId == userId }
    }

    override suspend fun softDeleteOrphans(userId: String, keepIds: List<String>, deletedAt: Long): Int {
        var changed = 0
        rows.replaceAll { id, row ->
            if (row.userId == userId && row.deletedAt == null && id !in keepIds) {
                changed++
                row.copy(deletedAt = deletedAt, updatedAt = deletedAt)
            } else {
                row
            }
        }
        return changed
    }
}

class FakeTransactionDao : TransactionDao {

    val rows = linkedMapOf<String, TransactionEntity>()

    override suspend fun upsert(transaction: TransactionEntity) {
        rows[transaction.id] = transaction
    }

    override suspend fun upsertAll(transactions: List<TransactionEntity>) {
        transactions.forEach { rows[it.id] = it }
    }

    override suspend fun findByIds(userId: String, ids: List<String>): List<TransactionEntity> =
        rows.values.filter { it.userId == userId && it.id in ids }

    override suspend fun softDeleteMissing(
        userId: String,
        fromInclusive: Long,
        toExclusive: Long,
        keepIds: List<String>,
        deletedAt: Long,
    ): Int = softDeleteRange(userId, fromInclusive, toExclusive, keepIds, deletedAt)

    override suspend fun softDeleteAllInRange(
        userId: String,
        fromInclusive: Long,
        toExclusive: Long,
        deletedAt: Long,
    ): Int = softDeleteRange(userId, fromInclusive, toExclusive, emptyList(), deletedAt)

    private fun softDeleteRange(
        userId: String,
        fromInclusive: Long,
        toExclusive: Long,
        keepIds: List<String>,
        deletedAt: Long,
    ): Int {
        var changed = 0
        rows.replaceAll { id, row ->
            val inRange = row.userId == userId && row.deletedAt == null && !row.pendingPush &&
                row.occurredAt >= fromInclusive && row.occurredAt < toExclusive
            if (inRange && id !in keepIds) {
                changed++
                row.copy(deletedAt = deletedAt, updatedAt = deletedAt)
            } else {
                row
            }
        }
        return changed
    }

    override fun observeRange(
        userId: String,
        fromInclusive: Long,
        toExclusive: Long,
    ): Flow<List<TransactionEntity>> = MutableStateFlow(
        rows.values
            .filter { it.userId == userId && it.deletedAt == null && it.occurredAt in fromInclusive until toExclusive }
            .sortedByDescending { it.occurredAt },
    )

    override fun observeRecent(userId: String, limit: Int): Flow<List<TransactionEntity>> =
        MutableStateFlow(rows.values.filter { it.userId == userId && it.deletedAt == null }.take(limit))

    override fun observeById(userId: String, id: String): Flow<TransactionEntity?> =
        MutableStateFlow(rows[id]?.takeIf { it.userId == userId && it.deletedAt == null })

    override suspend fun findById(userId: String, id: String): TransactionEntity? =
        rows[id]?.takeIf { it.userId == userId && it.deletedAt == null }

    override suspend fun update(
        userId: String,
        id: String,
        accountId: String,
        categoryId: String?,
        type: String,
        amountMinor: Long,
        currencyCode: String,
        occurredAt: Long,
        note: String?,
        updatedAt: Long,
    ): Int {
        val current = rows[id] ?: return 0
        if (current.userId != userId || current.deletedAt != null) return 0
        rows[id] = current.copy(
            accountId = accountId,
            categoryId = categoryId,
            type = type,
            amountMinor = amountMinor,
            currencyCode = currencyCode,
            occurredAt = occurredAt,
            note = note,
            updatedAt = updatedAt,
        )
        return 1
    }

    override suspend fun softDelete(userId: String, id: String, deletedAt: Long): Int {
        val current = rows[id] ?: return 0
        if (current.userId != userId || current.deletedAt != null) return 0
        rows[id] = current.copy(deletedAt = deletedAt, updatedAt = deletedAt)
        return 1
    }

    override fun observeTotal(
        userId: String,
        type: String,
        fromInclusive: Long,
        toExclusive: Long,
    ): Flow<Long> = MutableStateFlow(0L)

    override suspend fun deleteByUser(userId: String) {
        rows.values.removeAll { it.userId == userId }
    }
}

class FakeBudgetDao : BudgetDao {

    val rows = linkedMapOf<String, BudgetEntity>()

    override suspend fun upsert(budget: BudgetEntity) {
        rows[budget.id] = budget
    }

    override fun observeForPeriod(userId: String, periodStartEpochDay: Long): Flow<List<BudgetEntity>> =
        MutableStateFlow(rows.values.filter { it.userId == userId && it.periodStartEpochDay == periodStartEpochDay })

    override suspend fun deleteByUser(userId: String) {
        rows.values.removeAll { it.userId == userId }
    }
}

class FakeSavingsGoalDao : SavingsGoalDao {

    val goals = linkedMapOf<String, SavingsGoalEntity>()
    val contributions = linkedMapOf<String, GoalContributionEntity>()

    override suspend fun upsertGoal(goal: SavingsGoalEntity) {
        goals[goal.id] = goal
    }

    override suspend fun upsertContribution(contribution: GoalContributionEntity) {
        contributions[contribution.id] = contribution
    }

    override fun observeGoals(userId: String): Flow<List<SavingsGoalEntity>> =
        MutableStateFlow(goals.values.filter { it.userId == userId })

    override fun observeContributedTotal(goalId: String): Flow<Long> = MutableStateFlow(0L)

    override suspend fun deleteGoalsByUser(userId: String) {
        goals.values.removeAll { it.userId == userId }
    }

    override suspend fun deleteContributionsByUser(userId: String) {
        contributions.values.removeAll { it.userId == userId }
    }
}

class FakeRecurringRuleDao : RecurringRuleDao {

    val rows = linkedMapOf<String, RecurringRuleEntity>()

    override suspend fun upsert(rule: RecurringRuleEntity) {
        rows[rule.id] = rule
    }

    override fun observeActive(userId: String): Flow<List<RecurringRuleEntity>> =
        MutableStateFlow(rows.values.filter { it.userId == userId })

    override suspend fun deleteByUser(userId: String) {
        rows.values.removeAll { it.userId == userId }
    }
}

class FakeSessionStore : SessionStore {

    private val state = MutableStateFlow<String?>(null)

    val value: String? get() = state.value

    override val userId: Flow<String?> = state

    override suspend fun setUserId(userId: String) {
        state.value = userId
    }

    override suspend fun clear() {
        state.value = null
    }
}

class FakeSessionTokenStore : SessionTokenStore {

    private var token: SessionToken? = null

    override suspend fun read(): SessionToken? = token

    override suspend fun write(token: SessionToken) {
        this.token = token
    }

    override suspend fun clear() {
        token = null
    }
}

class FakeSyncMetadataStore : SyncMetadataStore {

    var value: SyncMetadata? = null

    override suspend fun read(): SyncMetadata? = value

    override suspend fun write(metadata: SyncMetadata) {
        value = metadata
    }

    override suspend fun clear() {
        value = null
    }
}

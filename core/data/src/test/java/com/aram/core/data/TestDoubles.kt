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

    override suspend fun countByUser(userId: String): Int =
        rows.values.count { it.userId == userId && it.deletedAt == null }

    override suspend fun deleteByUser(userId: String) {
        rows.values.removeAll { it.userId == userId }
    }
}

class FakeAccountDao : AccountDao {

    val rows = linkedMapOf<String, AccountEntity>()

    override suspend fun upsertAll(accounts: List<AccountEntity>) {
        accounts.forEach { rows[it.id] = it }
    }

    override fun observeByUser(userId: String): Flow<List<AccountEntity>> =
        MutableStateFlow(rows.values.filter { it.userId == userId && it.deletedAt == null })

    override suspend fun countByUser(userId: String): Int =
        rows.values.count { it.userId == userId && it.deletedAt == null }

    override suspend fun deleteByUser(userId: String) {
        rows.values.removeAll { it.userId == userId }
    }
}

class FakeTransactionDao : TransactionDao {

    val rows = linkedMapOf<String, TransactionEntity>()

    override suspend fun upsert(transaction: TransactionEntity) {
        rows[transaction.id] = transaction
    }

    override fun observeRecent(userId: String, limit: Int): Flow<List<TransactionEntity>> =
        MutableStateFlow(rows.values.filter { it.userId == userId }.take(limit))

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

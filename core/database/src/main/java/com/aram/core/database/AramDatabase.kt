package com.aram.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
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

@Database(
    entities = [
        UserEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        SavingsGoalEntity::class,
        GoalContributionEntity::class,
        RecurringRuleEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AramDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    abstract fun accountDao(): AccountDao

    abstract fun categoryDao(): CategoryDao

    abstract fun transactionDao(): TransactionDao

    abstract fun budgetDao(): BudgetDao

    abstract fun savingsGoalDao(): SavingsGoalDao

    abstract fun recurringRuleDao(): RecurringRuleDao

    companion object {
        const val NAME = "arya.db"
    }
}

package com.aram.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aram.core.database.dao.AccountDao
import com.aram.core.database.dao.BudgetDao
import com.aram.core.database.dao.CategoryDao
import com.aram.core.database.dao.LedgerWriteDao
import com.aram.core.database.dao.RecurringRuleDao
import com.aram.core.database.dao.SavingsGoalDao
import com.aram.core.database.dao.SyncOutboxDao
import com.aram.core.database.dao.TransactionDao
import com.aram.core.database.dao.UserDao
import com.aram.core.database.entity.AccountEntity
import com.aram.core.database.entity.BudgetEntity
import com.aram.core.database.entity.CategoryEntity
import com.aram.core.database.entity.GoalContributionEntity
import com.aram.core.database.entity.RecurringRuleEntity
import com.aram.core.database.entity.SavingsGoalEntity
import com.aram.core.database.entity.SyncOutboxEntity
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
        SyncOutboxEntity::class,
    ],
    version = 3,
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

    abstract fun syncOutboxDao(): SyncOutboxDao

    abstract fun ledgerWriteDao(): LedgerWriteDao

    companion object {
        const val NAME = "arya.db"

        /** Adds the outbox marker; v1 rows are treated as already on the server. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN pendingPush INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        /** Adds the offline write queue and the server category slug used to push. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN slug TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sync_outbox` (" +
                        "`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `entityId` TEXT NOT NULL, " +
                        "`op` TEXT NOT NULL, `payload` TEXT, `attempts` INTEGER NOT NULL DEFAULT 0, " +
                        "`nextAttemptAt` INTEGER NOT NULL DEFAULT 0, `lastError` TEXT, " +
                        "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_sync_outbox_userId_nextAttemptAt` " +
                        "ON `sync_outbox` (`userId`, `nextAttemptAt`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_sync_outbox_userId_entityId` " +
                        "ON `sync_outbox` (`userId`, `entityId`)",
                )
            }
        }

        val MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
    }
}

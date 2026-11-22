package com.aram.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AramDatabase =
        Room.databaseBuilder(context, AramDatabase::class.java, AramDatabase.NAME)
            .addMigrations(*AramDatabase.MIGRATIONS)
            .build()

    @Provides
    fun provideUserDao(database: AramDatabase) = database.userDao()

    @Provides
    fun provideAccountDao(database: AramDatabase) = database.accountDao()

    @Provides
    fun provideCategoryDao(database: AramDatabase) = database.categoryDao()

    @Provides
    fun provideTransactionDao(database: AramDatabase) = database.transactionDao()

    @Provides
    fun provideBudgetDao(database: AramDatabase) = database.budgetDao()

    @Provides
    fun provideSavingsGoalDao(database: AramDatabase) = database.savingsGoalDao()

    @Provides
    fun provideRecurringRuleDao(database: AramDatabase) = database.recurringRuleDao()

    @Provides
    fun provideSyncOutboxDao(database: AramDatabase) = database.syncOutboxDao()

    @Provides
    fun provideLedgerWriteDao(database: AramDatabase) = database.ledgerWriteDao()
}

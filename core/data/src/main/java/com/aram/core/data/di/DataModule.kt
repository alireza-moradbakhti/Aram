package com.aram.core.data.di

import com.aram.core.data.http.SessionTokenProvider
import com.aram.core.data.outbox.OutboxSyncImpl
import com.aram.core.data.outbox.WorkManagerOutboxTrigger
import com.aram.core.data.repository.AccountBalanceRepositoryImpl
import com.aram.core.data.repository.AuthRepositoryImpl
import com.aram.core.data.repository.BudgetRepositoryImpl
import com.aram.core.data.repository.LedgerSyncRepositoryImpl
import com.aram.core.data.repository.TransactionRepositoryImpl
import com.aram.core.data.sync.SyncScheduler
import com.aram.core.data.sync.WorkManagerSyncScheduler
import com.aram.core.datastore.SessionDataStore
import com.aram.core.datastore.SessionStore
import com.aram.core.datastore.SyncMetadataDataStore
import com.aram.core.datastore.SyncMetadataStore
import com.aram.core.domain.repository.AccountBalanceRepository
import com.aram.core.domain.repository.AuthRepository
import com.aram.core.domain.repository.BudgetRepository
import com.aram.core.domain.repository.LedgerDirectory
import com.aram.core.domain.repository.LedgerLookup
import com.aram.core.domain.repository.LedgerSync
import com.aram.core.domain.repository.OutboxSync
import com.aram.core.domain.repository.OutboxTrigger
import com.aram.core.domain.repository.TransactionRepository
import com.aram.core.network.http.TokenProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(implementation: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSessionStore(implementation: SessionDataStore): SessionStore

    @Binds
    @Singleton
    abstract fun bindSyncMetadataStore(implementation: SyncMetadataDataStore): SyncMetadataStore

    @Binds
    @Singleton
    abstract fun bindLedgerSync(implementation: LedgerSyncRepositoryImpl): LedgerSync

    @Binds
    @Singleton
    abstract fun bindSyncScheduler(implementation: WorkManagerSyncScheduler): SyncScheduler

    @Binds
    @Singleton
    abstract fun bindOutboxSync(implementation: OutboxSyncImpl): OutboxSync

    @Binds
    abstract fun bindOutboxTrigger(implementation: WorkManagerOutboxTrigger): OutboxTrigger

    @Binds
    @Singleton
    abstract fun bindTokenProvider(implementation: SessionTokenProvider): TokenProvider

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(implementation: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(implementation: BudgetRepositoryImpl): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindAccountBalanceRepository(
        implementation: AccountBalanceRepositoryImpl,
    ): AccountBalanceRepository

    @Binds
    @Singleton
    abstract fun bindLedgerLookup(implementation: TransactionRepositoryImpl): LedgerLookup

    @Binds
    @Singleton
    abstract fun bindLedgerDirectory(implementation: TransactionRepositoryImpl): LedgerDirectory
}

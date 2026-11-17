package com.aram.core.data.di

import com.aram.core.data.repository.AuthRepositoryImpl
import com.aram.core.data.repository.TransactionRepositoryImpl
import com.aram.core.datastore.SessionDataStore
import com.aram.core.datastore.SessionStore
import com.aram.core.domain.repository.AuthRepository
import com.aram.core.domain.repository.LedgerLookup
import com.aram.core.domain.repository.TransactionRepository
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
    abstract fun bindTransactionRepository(implementation: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindLedgerLookup(implementation: TransactionRepositoryImpl): LedgerLookup
}

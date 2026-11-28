package com.aram.app.di

import com.aram.core.domain.repository.LedgerLookup
import com.aram.core.domain.repository.TransactionRepository
import com.aram.core.domain.usecase.CreateTransaction
import com.aram.core.domain.usecase.CreateTransfer
import com.aram.core.domain.usecase.DeleteTransaction
import com.aram.core.domain.usecase.RestoreTransaction
import com.aram.core.domain.usecase.UpdateTransaction
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    @Singleton
    fun provideCreateTransaction(
        repository: TransactionRepository,
        lookup: LedgerLookup,
    ): CreateTransaction = CreateTransaction(repository, lookup)

    @Provides
    @Singleton
    fun provideCreateTransfer(
        repository: TransactionRepository,
        lookup: LedgerLookup,
    ): CreateTransfer = CreateTransfer(repository, lookup)

    @Provides
    @Singleton
    fun provideUpdateTransaction(
        repository: TransactionRepository,
        lookup: LedgerLookup,
    ): UpdateTransaction = UpdateTransaction(repository, lookup)

    @Provides
    @Singleton
    fun provideDeleteTransaction(
        repository: TransactionRepository,
    ): DeleteTransaction = DeleteTransaction(repository)

    @Provides
    @Singleton
    fun provideRestoreTransaction(
        repository: TransactionRepository,
    ): RestoreTransaction = RestoreTransaction(repository)

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}

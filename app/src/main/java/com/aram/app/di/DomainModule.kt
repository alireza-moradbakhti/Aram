package com.aram.app.di

import com.aram.core.domain.repository.LedgerLookup
import com.aram.core.domain.repository.TransactionRepository
import com.aram.core.domain.usecase.CreateTransaction
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
    fun provideClock(): Clock = Clock.systemDefaultZone()
}

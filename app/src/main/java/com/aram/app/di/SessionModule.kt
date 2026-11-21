package com.aram.app.di

import android.content.Context
import com.aram.app.BuildConfig
import com.aram.core.datastore.InMemorySessionTokenStore
import com.aram.core.datastore.KeystoreSessionTokenStore
import com.aram.core.datastore.SessionTokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SessionModule {

    /** Token expiry is 24 hours; a fixed UTC clock keeps tests deterministic. */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()

    /**
     * Alpha demos keep the token in memory so mock installs never touch the
     * Keystore; beta and production encrypt it at rest.
     */
    @Provides
    @Singleton
    fun provideSessionTokenStore(
        @ApplicationContext context: Context,
    ): SessionTokenStore = if (BuildConfig.BUILD_STAGE == "alpha") {
        InMemorySessionTokenStore()
    } else {
        KeystoreSessionTokenStore(context)
    }
}

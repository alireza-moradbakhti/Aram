package com.aram.app.di

import com.aram.app.BuildConfig
import com.aram.app.logging.AndroidLogSink
import com.aram.core.logging.LogConfig
import com.aram.core.logging.LogLevel
import com.aram.core.logging.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LoggingModule {

    @Provides
    @Singleton
    fun provideLogger(): Logger = Logger(
        config = LogConfig(
            minLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.WARN,
            // No backend yet: a flagged event stays local instead of leaving the device.
            serverUploadEnabled = false,
        ),
        sinks = listOf(AndroidLogSink()),
    )
}

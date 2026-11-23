package com.aram.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.aram.app.crash.AramCrashHandler
import com.aram.app.crash.CrashMarker
import com.aram.core.data.sync.SyncScheduler
import com.aram.core.domain.repository.OutboxTrigger
import com.aram.core.logging.Logger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AramApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var outboxTrigger: OutboxTrigger

    @Inject
    lateinit var syncScheduler: SyncScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        installCrashHandler()
        outboxTrigger.schedule()
        syncScheduler.schedulePeriodic()
        syncScheduler.syncNow()
        logger.i(
            "AramApplication",
            "Started ${BuildConfig.BUILD_STAGE} ${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_DATE})",
        )
    }

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(
            AramCrashHandler(
                marker = CrashMarker(this),
                logger = logger,
                delegate = previous,
            ),
        )
    }
}

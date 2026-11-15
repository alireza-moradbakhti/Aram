package com.aram.app

import android.app.Application
import com.aram.core.logging.Logger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AramApplication : Application() {

    @Inject
    lateinit var logger: Logger

    override fun onCreate() {
        super.onCreate()
        logger.i(
            "AramApplication",
            "Started ${BuildConfig.BUILD_STAGE} ${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_DATE})",
        )
    }
}

package com.aram.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aram.app.crash.CrashMarker
import com.aram.app.crash.RecoveryScreen
import com.aram.app.navigation.AramApp
import com.aram.app.session.MainViewModel
import com.aram.core.designsystem.AramTheme
import com.aram.core.designsystem.state.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var crashMarker: CrashMarker
    private var showRecovery = false

    override fun onCreate(savedInstanceState: Bundle?) {
        crashMarker = CrashMarker(this)
        showRecovery = crashMarker.hasPendingCrash()

        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            !showRecovery && viewModel.session.value is UiState.Loading
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AramTheme {
                if (showRecovery) {
                    RecoveryScreen(
                        onContinue = {
                            crashMarker.clear()
                            recreate()
                        },
                    )
                } else {
                    val session by viewModel.session.collectAsStateWithLifecycle()
                    AramApp(
                        sessionState = session,
                        onRetrySession = viewModel::retry,
                        onSignOut = viewModel::signOut,
                        showDebugActions = BuildConfig.DEBUG,
                    )
                }
            }
        }
    }
}

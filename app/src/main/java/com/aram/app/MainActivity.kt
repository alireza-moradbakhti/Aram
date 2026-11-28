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
import com.aram.app.session.TransactionUndoViewModel
import com.aram.core.designsystem.AramTheme
import com.aram.core.designsystem.state.UiState
import com.aram.core.network.config.ApiMode
import com.aram.core.network.mock.MockDemoAccount
import com.aram.core.network.mock.MockScenarioController
import com.aram.feature.onboarding.presentation.DemoCredentials
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val undoViewModel: TransactionUndoViewModel by viewModels()

    @Inject
    lateinit var mockScenarios: MockScenarioController

    private lateinit var crashMarker: CrashMarker
    private var showRecovery = false

    private val isMockBuild: Boolean
        get() = BuildConfig.API_MODE == ApiMode.MOCK.name

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
                    val scenario by mockScenarios.scenario.collectAsStateWithLifecycle()
                    AramApp(
                        sessionState = session,
                        onRetrySession = viewModel::retry,
                        onSignOut = viewModel::signOut,
                        onUndoDelete = undoViewModel::restore,
                        showDebugActions = BuildConfig.DEBUG,
                        debugScenario = scenario.takeIf { BuildConfig.DEBUG && isMockBuild },
                        onSelectDebugScenario = mockScenarios::select,
                        demoCredentials = if (isMockBuild) {
                            DemoCredentials(MockDemoAccount.EMAIL, MockDemoAccount.PASSWORD)
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

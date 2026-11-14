package com.aram.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import com.aram.app.navigation.AramApp
import com.aram.app.session.MainViewModel
import com.aram.app.session.SessionState
import com.aram.core.designsystem.AramTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            viewModel.session.value is SessionState.Loading
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AramTheme {
                val session by viewModel.session.collectAsStateWithLifecycle()
                AramApp(sessionState = session)
            }
        }
    }
}

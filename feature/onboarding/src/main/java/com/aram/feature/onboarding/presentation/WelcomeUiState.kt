package com.aram.feature.onboarding.presentation

import com.aram.core.designsystem.state.SubmitState

data class WelcomeUiState(
    val name: String = "",
    val submitState: SubmitState = SubmitState.Idle,
    val showNameError: Boolean = false,
)

package com.aram.feature.onboarding.presentation

data class WelcomeUiState(
    val name: String = "",
    val isSubmitting: Boolean = false,
    val showNameError: Boolean = false,
    val showSaveError: Boolean = false,
)

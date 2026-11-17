package com.aram.feature.onboarding.presentation

import com.aram.core.designsystem.state.SubmitState

data class WelcomeUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val acceptedTerms: Boolean = false,
    val submitState: SubmitState = SubmitState.Idle,
    val showNameError: Boolean = false,
    val showEmailError: Boolean = false,
    val showPasswordError: Boolean = false,
    val showTermsError: Boolean = false,
) {
    val passwordScore: Int = passwordStrengthScore(password)
}

/** Mirrors the prototype's strength meter: 0 empty, 1 weak, 2 fair, 3 strong. */
fun passwordStrengthScore(password: String): Int = when {
    password.length >= 8 && password.any(Char::isDigit) && password.any(Char::isLetter) -> 3
    password.length >= 6 -> 2
    password.isNotEmpty() -> 1
    else -> 0
}

private val EmailPattern = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")

internal fun isValidEmail(email: String): Boolean = EmailPattern.matches(email.trim())

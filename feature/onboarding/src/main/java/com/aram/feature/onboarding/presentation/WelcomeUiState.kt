package com.aram.feature.onboarding.presentation

import com.aram.core.designsystem.state.SubmitState

enum class AuthScreen {
    Welcome,
    SignIn,
    SignUp,
}

/** Alpha-only shortcut shown under the form; never populated in beta/production. */
data class DemoCredentials(
    val email: String,
    val password: String,
) {
    val hint: String
        get() = "Alpha demo · $email / $password"
}

enum class PasswordStrength { Empty, Weak, Fair, Strong }

data class WelcomeUiState(
    val screen: AuthScreen = AuthScreen.Welcome,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val termsAccepted: Boolean = true,
    val submitState: SubmitState = SubmitState.Idle,
    val fieldErrors: Map<String, String> = emptyMap(),
    val showNameError: Boolean = false,
) {
    val isSubmitting: Boolean
        get() = submitState is SubmitState.Submitting

    val passwordStrength: PasswordStrength
        get() = strengthOf(password)
}

fun strengthOf(password: String): PasswordStrength {
    if (password.isEmpty()) return PasswordStrength.Empty
    val signals = listOf(
        password.length >= 8,
        password.length >= 12,
        password.any { it.isLowerCase() } && password.any { it.isDigit() },
        password.any { it.isUpperCase() } || password.any { !it.isLetterOrDigit() },
    ).count { it }
    return when {
        signals <= 1 -> PasswordStrength.Weak
        signals <= 3 -> PasswordStrength.Fair
        else -> PasswordStrength.Strong
    }
}

package com.aram.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aram.core.designsystem.state.SubmitState
import com.aram.core.domain.repository.AuthRepository
import com.aram.core.model.AppError
import com.aram.core.model.Outcome
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) {
        _uiState.update {
            it.copy(name = name, showNameError = false, submitState = SubmitState.Idle)
                .withoutFieldError("displayName")
        }
    }

    fun onEmailChange(email: String) {
        _uiState.update {
            it.copy(email = email, submitState = SubmitState.Idle).withoutFieldError("email")
        }
    }

    fun onPasswordChange(password: String) {
        _uiState.update {
            it.copy(password = password, submitState = SubmitState.Idle).withoutFieldError("password")
        }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun onTermsChange(accepted: Boolean) {
        _uiState.update {
            it.copy(termsAccepted = accepted).withoutFieldError("terms")
        }
    }

    fun onOpenSignIn() {
        _uiState.update {
            it.copy(screen = AuthScreen.SignIn, submitState = SubmitState.Idle, fieldErrors = emptyMap())
        }
    }

    fun onOpenSignUp() {
        _uiState.update {
            it.copy(screen = AuthScreen.SignUp, submitState = SubmitState.Idle, fieldErrors = emptyMap())
        }
    }

    fun onBack() {
        _uiState.update {
            it.copy(
                screen = AuthScreen.Welcome,
                password = "",
                passwordVisible = false,
                submitState = SubmitState.Idle,
                fieldErrors = emptyMap(),
            )
        }
    }

    fun onUseDemo(credentials: DemoCredentials) {
        _uiState.update {
            it.copy(
                screen = AuthScreen.SignIn,
                email = credentials.email,
                password = credentials.password,
                passwordVisible = false,
                submitState = SubmitState.Idle,
                fieldErrors = emptyMap(),
            )
        }
    }

    fun onCreateProfile() {
        val current = _uiState.value
        if (current.isSubmitting) return

        val name = current.name.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(showNameError = true) }
            return
        }

        submit { authRepository.createLocalProfile(name) }
    }

    fun onSubmit() {
        val current = _uiState.value
        if (current.isSubmitting) return

        when (current.screen) {
            AuthScreen.SignUp -> {
                if (!current.termsAccepted) {
                    _uiState.update {
                        it.copy(fieldErrors = it.fieldErrors + ("terms" to TERMS_REQUIRED))
                    }
                    return
                }
                submit {
                    authRepository.register(
                        displayName = current.name,
                        email = current.email,
                        password = current.password,
                    )
                }
            }

            AuthScreen.SignIn -> submit {
                authRepository.signIn(email = current.email, password = current.password)
            }

            AuthScreen.Welcome -> Unit
        }
    }

    private fun submit(call: suspend () -> Outcome<*>) {
        _uiState.update { it.copy(submitState = SubmitState.Submitting, fieldErrors = emptyMap()) }
        viewModelScope.launch {
            when (val result = call()) {
                is Outcome.Success -> Unit
                is Outcome.Failure -> _uiState.update { state ->
                    val error = result.error
                    if (error is AppError.Validation) {
                        state.copy(submitState = SubmitState.Idle, fieldErrors = error.fields)
                    } else {
                        state.copy(submitState = SubmitState.Failed(error))
                    }
                }
            }
        }
    }

    private fun WelcomeUiState.withoutFieldError(key: String): WelcomeUiState =
        if (key in fieldErrors) copy(fieldErrors = fieldErrors - key) else this

    private companion object {
        const val TERMS_REQUIRED = "Accept the Terms to continue"
    }
}

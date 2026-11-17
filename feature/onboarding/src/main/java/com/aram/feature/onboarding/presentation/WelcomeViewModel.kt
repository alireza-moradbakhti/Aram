package com.aram.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aram.core.designsystem.state.SubmitState
import com.aram.core.domain.repository.AuthRepository
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
        }
    }

    fun onEmailChange(email: String) {
        _uiState.update {
            it.copy(email = email, showEmailError = false, submitState = SubmitState.Idle)
        }
    }

    fun onPasswordChange(password: String) {
        _uiState.update {
            it.copy(password = password, showPasswordError = false, submitState = SubmitState.Idle)
        }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun onAcceptedTermsChange(accepted: Boolean) {
        _uiState.update {
            it.copy(acceptedTerms = accepted, showTermsError = false, submitState = SubmitState.Idle)
        }
    }

    fun onCreateAccount() {
        val current = _uiState.value
        if (current.submitState is SubmitState.Submitting) return

        val name = current.name.trim()
        val email = current.email.trim()
        val passwordError = passwordStrengthScore(current.password) < 3
        val nameError = name.isEmpty()
        val emailError = !isValidEmail(email)

        if (nameError || emailError || passwordError || !current.acceptedTerms) {
            _uiState.update {
                it.copy(
                    showNameError = nameError,
                    showEmailError = emailError,
                    showPasswordError = passwordError,
                    showTermsError = !current.acceptedTerms,
                )
            }
            return
        }

        _uiState.update { it.copy(submitState = SubmitState.Submitting) }
        viewModelScope.launch {
            when (val result = authRepository.createLocalProfile(name)) {
                is Outcome.Success -> Unit
                is Outcome.Failure -> _uiState.update {
                    it.copy(submitState = SubmitState.Failed(result.error))
                }
            }
        }
    }
}

package com.aram.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.aram.core.domain.repository.AuthRepository
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
            it.copy(name = name, showNameError = false, showSaveError = false)
        }
    }

    fun onCreateProfile() {
        val name = _uiState.value.name.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(showNameError = true) }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, showSaveError = false) }
        viewModelScope.launch {
            runCatching { authRepository.createLocalProfile(name) }
                .onFailure {
                    _uiState.update {
                        it.copy(isSubmitting = false, showSaveError = true)
                    }
                }
        }
    }
}

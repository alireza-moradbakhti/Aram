package com.aram.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
)

@HiltViewModel
class DashboardViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        // Placeholder until the dashboard reads the cache-first repositories
        // (AR-013/AR-018); the skeleton must come from state, not the screen.
        viewModelScope.launch {
            delay(DEMO_LOAD_MILLIS)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private companion object {
        const val DEMO_LOAD_MILLIS = 650L
    }
}

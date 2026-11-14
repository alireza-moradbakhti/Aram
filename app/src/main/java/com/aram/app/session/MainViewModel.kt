package com.aram.app.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.aram.core.domain.repository.AuthRepository
import com.aram.core.model.UserProfile
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data class Ready(val profile: UserProfile) : SessionState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    val session: StateFlow<SessionState> = authRepository.observeSession()
        .map { profile ->
            if (profile == null) SessionState.LoggedOut else SessionState.Ready(profile)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SessionState.Loading,
        )
}

package com.aram.app.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aram.core.designsystem.state.UiState
import com.aram.core.domain.repository.AuthRepository
import com.aram.core.model.Outcome
import com.aram.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val retryTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val session: StateFlow<UiState<UserProfile>> = retryTrigger
        .flatMapLatest {
            authRepository.observeSession().map<Outcome<UserProfile?>, UiState<UserProfile>> { outcome ->
                when (outcome) {
                    is Outcome.Success -> outcome.value
                        ?.let { UiState.Content(it) }
                        ?: UiState.Empty

                    is Outcome.Failure -> UiState.Error(outcome.error)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UiState.Loading,
        )

    fun retry() {
        retryTrigger.update { it + 1 }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}

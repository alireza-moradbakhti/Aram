package com.aram.core.designsystem.state

import com.aram.core.model.AppError

/**
 * State of a form submission. Unlike [UiState], the form stays on screen while
 * it carries a failure, so the user never loses what they typed.
 */
sealed interface SubmitState {

    data object Idle : SubmitState

    data object Submitting : SubmitState

    data class Failed(val error: AppError) : SubmitState
}

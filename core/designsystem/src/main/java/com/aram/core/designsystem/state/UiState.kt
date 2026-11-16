package com.aram.core.designsystem.state

import com.aram.core.model.AppError

/**
 * The one presentation state every content screen renders.
 *
 * Offline is a kind of [AppError] until the network layer exists; a cached
 * content + offline banner arrives with the cache-first work (AR-041).
 */
sealed interface UiState<out T> {

    data object Loading : UiState<Nothing>

    data class Content<T>(val data: T) : UiState<T>

    data object Empty : UiState<Nothing>

    data class Error(val error: AppError) : UiState<Nothing>
}

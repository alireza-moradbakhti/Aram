package com.aram.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aram.core.designsystem.R
import com.aram.core.model.AppError

/**
 * Calm failure surface: a title, a short message, and an optional retry.
 * Never renders a throwable message or stack trace.
 */
@Composable
fun ErrorState(
    error: AppError,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(error.titleRes()),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(error.messageRes()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.error_action_retry))
            }
        }
    }
}

@StringRes
private fun AppError.titleRes(): Int = when (this) {
    is AppError.Offline -> R.string.error_offline_title
    is AppError.Timeout -> R.string.error_timeout_title
    is AppError.Unauthorized, is AppError.Forbidden -> R.string.error_session_title
    is AppError.Permission -> R.string.error_permission_title
    is AppError.Validation -> R.string.error_validation_title
    is AppError.Server, is AppError.RateLimited -> R.string.error_server_title
    else -> R.string.error_generic_title
}

@StringRes
private fun AppError.messageRes(): Int = when (this) {
    is AppError.Offline -> R.string.error_offline_message
    is AppError.Timeout -> R.string.error_timeout_message
    is AppError.Unauthorized, is AppError.Forbidden -> R.string.error_session_message
    is AppError.Permission -> R.string.error_permission_message
    is AppError.Validation -> R.string.error_validation_message
    is AppError.Server, is AppError.RateLimited -> R.string.error_server_message
    else -> R.string.error_generic_message
}

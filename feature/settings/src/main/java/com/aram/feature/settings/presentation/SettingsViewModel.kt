package com.aram.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aram.core.domain.repository.LedgerSync
import com.aram.core.domain.repository.OutboxSync
import com.aram.core.model.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Sync health for the profile header and the manual "Sync now" action. */
data class SyncStatusUi(
    val status: SyncStatus = SyncStatus.IDLE,
    val lastSyncedAtMillis: Long? = null,
    val pending: Int = 0,
    val failed: Int = 0,
    val nowMillis: Long = 0L,
) {
    val isSyncing: Boolean get() = status == SyncStatus.REFRESHING
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val ledgerSync: LedgerSync,
    outboxSync: OutboxSync,
    clock: Clock,
) : ViewModel() {

    val syncStatus: StateFlow<SyncStatusUi> =
        combine(ledgerSync.state, outboxSync.snapshot) { sync, outbox ->
            SyncStatusUi(
                status = sync.status,
                lastSyncedAtMillis = sync.lastSyncedAtMillis,
                pending = outbox.pending,
                failed = outbox.failed,
                nowMillis = clock.millis(),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SyncStatusUi(nowMillis = clock.millis()),
        )

    fun onSyncNow() {
        viewModelScope.launch { ledgerSync.syncNow() }
    }
}

internal fun syncSummary(ui: SyncStatusUi): String {
    val base = when (ui.status) {
        SyncStatus.REFRESHING -> "Syncing…"
        SyncStatus.OFFLINE -> "Offline · showing cached data"
        SyncStatus.FAILED -> "Sync failed"
        SyncStatus.STALE -> "Showing older data"
        SyncStatus.IDLE -> lastSyncedLabel(ui.lastSyncedAtMillis, ui.nowMillis)
    }
    val queue = when {
        ui.failed > 0 -> "${ui.failed} couldn't sync"
        ui.pending > 0 -> "${ui.pending} waiting"
        else -> null
    }
    return if (queue == null) base else "$base · $queue"
}

private fun lastSyncedLabel(lastSyncedAtMillis: Long?, nowMillis: Long): String {
    if (lastSyncedAtMillis == null) return "Not synced yet"
    val minutes = (nowMillis - lastSyncedAtMillis).coerceAtLeast(0L) / 60_000L
    return when {
        minutes < 1 -> "Synced just now"
        minutes < 60 -> "Synced ${minutes}m ago"
        minutes < 24 * 60 -> "Synced ${minutes / 60}h ago"
        else -> "Synced ${minutes / (24 * 60)}d ago"
    }
}

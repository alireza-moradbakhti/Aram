package com.aram.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsRoute(
    displayName: String,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    showDebugActions: Boolean = false,
    debugScenarios: List<DebugScenarioOption> = emptyList(),
    activeDebugScenarioId: String? = null,
    onSelectDebugScenario: (String) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    SettingsScreen(
        displayName = displayName,
        onSignOut = onSignOut,
        showDebugActions = showDebugActions,
        debugScenarios = debugScenarios,
        activeDebugScenarioId = activeDebugScenarioId,
        onSelectDebugScenario = onSelectDebugScenario,
        syncStatus = syncStatus,
        onSyncNow = viewModel::onSyncNow,
        modifier = modifier,
    )
}

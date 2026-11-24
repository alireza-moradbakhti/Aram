package com.aram.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aram.core.designsystem.component.AramBackground
import com.aram.core.designsystem.component.AramBottomBar
import com.aram.core.designsystem.component.AramFab
import com.aram.core.designsystem.component.AramTab
import com.aram.core.designsystem.component.AramToast
import com.aram.core.designsystem.component.ErrorState
import com.aram.core.designsystem.icon.AramIcons
import com.aram.core.designsystem.state.UiState
import com.aram.core.model.UserProfile
import com.aram.core.network.mock.MockScenario
import com.aram.feature.budgets.presentation.BudgetsScreen
import com.aram.feature.dashboard.presentation.DashboardRoute
import com.aram.feature.onboarding.presentation.AramSplashScreen
import com.aram.feature.onboarding.presentation.DemoCredentials
import com.aram.feature.onboarding.presentation.EntryRoute
import com.aram.feature.settings.presentation.DebugScenarioOption
import com.aram.feature.settings.presentation.SettingsRoute
import com.aram.feature.transactions.presentation.AddTransactionRoute
import com.aram.feature.transactions.presentation.TransactionsRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ADD_TRANSACTION_ROUTE = "add-transaction"
private const val SPLASH_MILLIS = 650L

@Composable
fun AramApp(
    sessionState: UiState<UserProfile>,
    onRetrySession: () -> Unit,
    onSignOut: () -> Unit,
    showDebugActions: Boolean,
    debugScenario: MockScenario? = null,
    onSelectDebugScenario: (MockScenario) -> Unit = {},
    demoCredentials: DemoCredentials? = null,
    modifier: Modifier = Modifier,
) {
    var splashElapsed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!splashElapsed) {
            delay(SPLASH_MILLIS)
            splashElapsed = true
        }
    }

    if (!splashElapsed || sessionState is UiState.Loading) {
        AramSplashScreen(modifier = modifier)
        return
    }

    when (sessionState) {
        UiState.Loading -> AramSplashScreen(modifier = modifier)

        UiState.Empty -> EntryRoute(
            demoCredentials = demoCredentials,
            modifier = modifier,
        )

        is UiState.Content -> MainScaffold(
            profile = sessionState.data,
            onSignOut = onSignOut,
            showDebugActions = showDebugActions,
            debugScenario = debugScenario,
            onSelectDebugScenario = onSelectDebugScenario,
            modifier = modifier,
        )

        is UiState.Error -> AramBackground(modifier = modifier) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                ErrorState(
                    error = sessionState.error,
                    onRetry = onRetrySession,
                )
            }
        }
    }
}

private enum class MainTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Home("home", "Home", AramIcons.home),
    Activity("activity", "Activity", AramIcons.activity),
    Budgets("budgets", "Budgets", AramIcons.budgets),
    Profile("profile", "Profile", AramIcons.profile),
}

private val tabs = listOf(
    AramTab(MainTab.Home.label, MainTab.Home.icon),
    AramTab(MainTab.Activity.label, MainTab.Activity.icon),
    AramTab(MainTab.Budgets.label, MainTab.Budgets.icon),
    AramTab(MainTab.Profile.label, MainTab.Profile.icon),
)

@Composable
private fun MainScaffold(
    profile: UserProfile,
    onSignOut: () -> Unit,
    showDebugActions: Boolean,
    debugScenario: MockScenario?,
    onSelectDebugScenario: (MockScenario) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isAddRoute = currentDestination?.route == ADD_TRANSACTION_ROUTE
    val currentTab = MainTab.entries.firstOrNull { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.route } == true
    } ?: MainTab.Home

    fun navigateTo(tab: MainTab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    AramBackground(modifier = modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    AramToast(
                        message = data.visuals.message,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    )
                }
            },
            bottomBar = {
                if (!isAddRoute) {
                    Box(Modifier.navigationBarsPadding()) {
                        AramBottomBar(
                            tabs = tabs,
                            selectedIndex = currentTab.ordinal,
                            onSelect = { index -> navigateTo(MainTab.entries[index]) },
                        )
                    }
                }
            },
            floatingActionButton = {
                if ((currentTab == MainTab.Home || currentTab == MainTab.Activity) && !isAddRoute) {
                    AramFab(
                        onClick = { navController.navigate(ADD_TRANSACTION_ROUTE) },
                        contentDescription = "Add transaction",
                    )
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = MainTab.Home.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(MainTab.Home.route) {
                    DashboardRoute(
                        displayName = profile.displayName,
                        onSeeAll = { navigateTo(MainTab.Activity) },
                    )
                }
                composable(MainTab.Activity.route) {
                    TransactionsRoute(
                        onAddTransaction = { navController.navigate(ADD_TRANSACTION_ROUTE) },
                    )
                }
                composable(MainTab.Budgets.route) {
                    BudgetsScreen()
                }
                composable(MainTab.Profile.route) {
                    SettingsRoute(
                        displayName = profile.displayName,
                        showDebugActions = showDebugActions,
                        debugScenarios = if (debugScenario == null) {
                            emptyList()
                        } else {
                            MockScenario.entries.map { scenario ->
                                DebugScenarioOption(
                                    id = scenario.name,
                                    label = scenario.label,
                                    isFailure = scenario.isFailure,
                                )
                            }
                        },
                        activeDebugScenarioId = debugScenario?.name,
                        onSelectDebugScenario = { id ->
                            MockScenario.entries
                                .firstOrNull { it.name == id }
                                ?.let(onSelectDebugScenario)
                        },
                        onSignOut = onSignOut,
                    )
                }
                composable(ADD_TRANSACTION_ROUTE) {
                    AddTransactionRoute(
                        onClose = { navController.popBackStack() },
                        onSaved = {
                            navController.popBackStack()
                            navigateTo(MainTab.Activity)
                            scope.launch { snackbarHostState.showSnackbar("Transaction saved") }
                        },
                    )
                }
            }
        }
    }
}

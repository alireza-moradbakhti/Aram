package com.aram.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aram.app.session.SessionState
import com.aram.core.model.UserProfile
import com.aram.feature.budgets.presentation.BudgetsScreen
import com.aram.feature.dashboard.presentation.DashboardRoute
import com.aram.feature.goals.presentation.GoalsScreen
import com.aram.feature.onboarding.presentation.WelcomeRoute
import com.aram.feature.settings.presentation.SettingsScreen
import com.aram.feature.transactions.presentation.TransactionsScreen

@Composable
fun AramApp(
    sessionState: SessionState,
    modifier: Modifier = Modifier,
) {
    when (sessionState) {
        SessionState.Loading -> Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        )

        SessionState.LoggedOut -> WelcomeRoute(modifier = modifier)

        is SessionState.Ready -> MainScaffold(
            profile = sessionState.profile,
            modifier = modifier,
        )
    }
}

private enum class MainTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Home("home", "Home", Icons.Filled.Home),
    Transactions("transactions", "Transactions", Icons.AutoMirrored.Filled.List),
    Budgets("budgets", "Budgets", Icons.Filled.DateRange),
    Goals("goals", "Goals", Icons.Filled.Star),
    Settings("settings", "Settings", Icons.Filled.Settings),
}

@Composable
private fun MainScaffold(
    profile: UserProfile,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    val selected = currentDestination
                        ?.hierarchy
                        ?.any { destination -> destination.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                            )
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainTab.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(MainTab.Home.route) {
                DashboardRoute(displayName = profile.displayName)
            }
            composable(MainTab.Transactions.route) {
                TransactionsScreen()
            }
            composable(MainTab.Budgets.route) {
                BudgetsScreen()
            }
            composable(MainTab.Goals.route) {
                GoalsScreen()
            }
            composable(MainTab.Settings.route) {
                SettingsScreen(displayName = profile.displayName)
            }
        }
    }
}

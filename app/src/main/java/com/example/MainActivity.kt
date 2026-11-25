package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.island.CalibrationScreen
import com.example.island.FeaturePickerScreen
import com.example.island.IslandService
import com.example.keepalive.IslandKeepAliveService
import com.example.notifications.NotificationHistoryScreen
import com.example.setup.AlertsTabScreen
import com.example.setup.HomeHubScreen
import com.example.setup.MusicTabScreen
import com.example.setup.NotesTabScreen
import com.example.setup.SearchTabScreen
import com.example.setup.SettingsGroup
import com.example.setup.SettingsGroupDetailScreen
import com.example.setup.SettingsHomeScreen
import com.example.setup.SetupWizardScreen
import com.example.setup.ShelfTabScreen
import com.example.setup.TimerTabScreen
import com.example.setup.ToolsTabScreen
import com.example.system.AccentTheme
import com.example.system.IslandSettings
import com.example.ui.theme.DynamicIslandTheme
import com.example.ui.theme.FloatingGlassBottomDock
import com.example.ui.theme.IslandColors
import com.example.ui.theme.MainDockTab
import kotlinx.serialization.Serializable

@Serializable object RouteHome
@Serializable object RouteNotes
@Serializable object RouteAlerts
@Serializable object RouteShelf
@Serializable object RouteTools
@Serializable object RouteMusic
@Serializable data class RouteTimer(val initialMode: Int = 0)
@Serializable object RouteSearch
@Serializable object RouteSettingsHome

@Serializable object RouteWizard
@Serializable object RouteCalibration
@Serializable object RouteFeaturePicker
@Serializable object RouteNotificationHistory
@Serializable data class RouteSettingsGroup(val groupName: String)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        val app = application as DynamicIslandApp
        val initialOpenRoute = intent?.getStringExtra("open_route")

        setContent {
            val settings by app.settingsRepository.settingsFlow.collectAsState(initial = IslandSettings())
            val isServiceRunning by IslandService.isRunning.collectAsState()
            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner, settings.islandEnabled, isServiceRunning) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        app.mediaHub.start()
                        app.quickTogglesManager.refreshSystemStates()
                        val canDraw = Settings.canDrawOverlays(this@MainActivity)
                        if (settings.islandEnabled && (isServiceRunning || canDraw)) {
                            IslandKeepAliveService.start(this@MainActivity)
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(isServiceRunning, settings.islandEnabled) {
                val canDraw = Settings.canDrawOverlays(this@MainActivity)
                if (settings.islandEnabled && (isServiceRunning || canDraw)) {
                    IslandKeepAliveService.start(this@MainActivity)
                }
            }

            LaunchedEffect(settings.clipboardHistoryEnabled) {
                app.clipboardMonitor.setForegroundActive(settings.clipboardHistoryEnabled)
            }

            val accentOverride: Color = IslandColors.resolveAccentColor(this@MainActivity, settings.accentTheme)

            DynamicIslandTheme(
                darkTheme = true,
                dynamicColor = false,
                accentOverride = accentOverride
            ) {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination

                val activeDockTab: MainDockTab? = when {
                    currentDestination == null -> MainDockTab.HOME
                    currentDestination.hasRoute<RouteHome>() -> MainDockTab.HOME
                    currentDestination.hasRoute<RouteNotes>() -> MainDockTab.NOTES
                    currentDestination.hasRoute<RouteAlerts>() -> MainDockTab.ALERTS
                    currentDestination.hasRoute<RouteShelf>() -> MainDockTab.SHELF
                    currentDestination.hasRoute<RouteTools>() -> MainDockTab.TOOLS
                    currentDestination.hasRoute<RouteMusic>() -> MainDockTab.MUSIC
                    currentDestination.hasRoute<RouteTimer>() -> MainDockTab.TIMER
                    currentDestination.hasRoute<RouteSearch>() -> MainDockTab.SEARCH
                    currentDestination.hasRoute<RouteSettingsHome>() -> MainDockTab.SETTINGS
                    else -> null
                }

                fun navigateDockTab(tab: MainDockTab, timerInitialMode: Int = 0) {
                    if (tab == MainDockTab.HOME) {
                        val popped = navController.popBackStack<RouteHome>(inclusive = false)
                        if (!popped) {
                            navController.navigate(RouteHome) {
                                launchSingleTop = true
                            }
                        }
                        return
                    }
                    val target: Any = when (tab) {
                        MainDockTab.HOME -> RouteHome
                        MainDockTab.NOTES -> RouteNotes
                        MainDockTab.ALERTS -> RouteAlerts
                        MainDockTab.SHELF -> RouteShelf
                        MainDockTab.TOOLS -> RouteTools
                        MainDockTab.MUSIC -> RouteMusic
                        MainDockTab.TIMER -> RouteTimer(initialMode = timerInitialMode)
                        MainDockTab.SEARCH -> RouteSearch
                        MainDockTab.SETTINGS -> RouteSettingsHome
                    }
                    navController.navigate(target) {
                        popUpTo<RouteHome> {
                            saveState = timerInitialMode == 0
                        }
                        launchSingleTop = true
                        restoreState = timerInitialMode == 0
                    }
                }

                LaunchedEffect(initialOpenRoute) {
                    if (initialOpenRoute == "feature_picker") {
                        navController.navigate(RouteFeaturePicker) {
                            launchSingleTop = true
                        }
                    }
                }

                Scaffold(
                    containerColor = IslandColors.ObsidianBg,
                    contentWindowInsets = WindowInsets(0),
                    bottomBar = {
                        if (activeDockTab != null) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FloatingGlassBottomDock(
                                    selectedTab = activeDockTab,
                                    onSelectTab = { tab -> navigateDockTab(tab) },
                                    modifier = Modifier.widthIn(max = 600.dp)
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        NavHost(
                        navController = navController,
                        startDestination = RouteHome,
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None }
                    ) {
                        composable<RouteHome> {
                            HomeHubScreen(
                                app = app,
                                onSelectDockTab = { tab -> navigateDockTab(tab) },
                                onOpenSettings = {
                                    navController.navigate(RouteSettingsHome) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenWizard = { navController.navigate(RouteWizard) },
                                onOpenCalibration = { navController.navigate(RouteCalibration) },
                                onOpenFeaturePicker = { navController.navigate(RouteFeaturePicker) },
                                onOpenNotifications = { navController.navigate(RouteNotificationHistory) }
                            )
                        }

                        composable<RouteNotes> {
                            NotesTabScreen(app = app)
                        }

                        composable<RouteAlerts> {
                            AlertsTabScreen(app = app)
                        }

                        composable<RouteShelf> {
                            ShelfTabScreen(
                                app = app,
                                onOpenWizard = { navController.navigate(RouteWizard) }
                            )
                        }

                        composable<RouteTools> {
                            ToolsTabScreen(
                                app = app,
                                onOpenStopwatchTab = { navigateDockTab(MainDockTab.TIMER, timerInitialMode = 2) }
                            )
                        }

                        composable<RouteMusic> {
                            MusicTabScreen(app = app)
                        }

                        composable<RouteTimer> { backEntry ->
                            val timerRoute = backEntry.toRoute<RouteTimer>()
                            TimerTabScreen(
                                app = app,
                                initialMode = timerRoute.initialMode
                            )
                        }

                        composable<RouteSearch> {
                            SearchTabScreen(app = app)
                        }

                        composable<RouteSettingsHome> {
                            SettingsHomeScreen(
                                app = app,
                                onBack = {
                                    if (!navController.popBackStack()) {
                                        navigateDockTab(MainDockTab.HOME)
                                    }
                                },
                                onOpenGroup = { group ->
                                    navController.navigate(RouteSettingsGroup(group.name))
                                }
                            )
                        }

                        composable<RouteWizard> {
                            SetupWizardScreen(
                                app = app,
                                onDone = { navController.popBackStack() }
                            )
                        }

                        composable<RouteCalibration> {
                            CalibrationScreen(
                                app = app,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<RouteFeaturePicker> {
                            FeaturePickerScreen(
                                app = app,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<RouteNotificationHistory> {
                            NotificationHistoryScreen(
                                app = app,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<RouteSettingsGroup> { backStackEntry ->
                            val route = backStackEntry.toRoute<RouteSettingsGroup>()
                            val group = runCatching { SettingsGroup.valueOf(route.groupName) }
                                .getOrDefault(SettingsGroup.LOOK)
                            SettingsGroupDetailScreen(
                                app = app,
                                group = group,
                                onBack = { navController.popBackStack() },
                                onOpenCalibration = { navController.navigate(RouteCalibration) },
                                onOpenFeaturePicker = { navController.navigate(RouteFeaturePicker) },
                                onOpenNotificationHistory = { navController.navigate(RouteNotificationHistory) },
                                onOpenWizard = { navController.navigate(RouteWizard) }
                            )
                        }
                    }
                }
            }
        }
    }
}
}



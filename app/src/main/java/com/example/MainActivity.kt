package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.island.CalibrationScreen
import com.example.island.FeaturePickerScreen
import com.example.island.IslandService
import com.example.keepalive.IslandKeepAliveService
import com.example.notifications.NotificationHistoryScreen
import com.example.setup.HomeHubScreen
import com.example.setup.SettingsGroup
import com.example.setup.SettingsGroupDetailScreen
import com.example.setup.SettingsHomeScreen
import com.example.setup.SetupWizardScreen
import com.example.system.AccentTheme
import com.example.system.IslandSettings
import com.example.ui.theme.DynamicIslandTheme
import com.example.ui.theme.IslandColors
import com.example.ui.theme.LocalAppBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.serialization.Serializable

@Serializable object RouteHome
@Serializable object RouteWizard
@Serializable object RouteCalibration
@Serializable object RouteFeaturePicker
@Serializable object RouteNotificationHistory
@Serializable object RouteSettingsHome
@Serializable data class RouteSettingsGroup(val groupName: String)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as DynamicIslandApp
        val initialOpenRoute = intent?.getStringExtra("open_route")

        setContent {
            val settings by app.settingsRepository.settingsFlow.collectAsState(initial = IslandSettings())
            val isServiceRunning by IslandService.isRunning.collectAsState()

            LaunchedEffect(isServiceRunning) {
                if (isServiceRunning) {
                    IslandKeepAliveService.start(this@MainActivity)
                }
            }

            LaunchedEffect(settings.clipboardHistoryEnabled) {
                app.clipboardMonitor.setForegroundActive(settings.clipboardHistoryEnabled)
            }

            val accentOverride: Color? = when (settings.accentTheme) {
                AccentTheme.ORANGE -> IslandColors.AccentOrange
                AccentTheme.CYAN -> IslandColors.AccentCyan
                AccentTheme.GREEN -> IslandColors.AccentGreen
                AccentTheme.DYNAMIC -> null
            }

            DynamicIslandTheme(
                darkTheme = true,
                dynamicColor = settings.accentTheme == AccentTheme.DYNAMIC,
                accentOverride = accentOverride
            ) {
                val appBackdrop = rememberLayerBackdrop()
                val navController = rememberNavController()

                LaunchedEffect(initialOpenRoute) {
                    if (initialOpenRoute == "feature_picker") {
                        navController.navigate(RouteFeaturePicker)
                    }
                }

                CompositionLocalProvider(LocalAppBackdrop provides appBackdrop) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(appBackdrop)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = RouteHome
                        ) {
                            composable<RouteHome> {
                                HomeHubScreen(
                                    app = app,
                                    onOpenSettings = { navController.navigate(RouteSettingsHome) },
                                    onOpenWizard = { navController.navigate(RouteWizard) },
                                    onOpenCalibration = { navController.navigate(RouteCalibration) },
                                    onOpenFeaturePicker = { navController.navigate(RouteFeaturePicker) },
                                    onOpenNotifications = { navController.navigate(RouteNotificationHistory) }
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

                            composable<RouteSettingsHome> {
                                SettingsHomeScreen(
                                    app = app,
                                    onBack = { navController.popBackStack() },
                                    onOpenGroup = { group ->
                                        navController.navigate(RouteSettingsGroup(group.name))
                                    }
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

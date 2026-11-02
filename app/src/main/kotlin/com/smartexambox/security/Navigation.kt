package com.smartexambox.security

import androidx.compose.runtime.Composable
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smartexambox.security.ui.MainSecurityViewModel
import com.smartexambox.security.ui.ScreenRoutes
import com.smartexambox.security.ui.screens.AboutProjectScreen
import com.smartexambox.security.ui.screens.AdminDashboardScreen
import com.smartexambox.security.ui.screens.AlertsNotificationScreen
import com.smartexambox.security.ui.screens.BiometricAuthScreen
import com.smartexambox.security.ui.screens.DashboardScreen
import com.smartexambox.security.ui.screens.DeviceRegistrationScreen
import com.smartexambox.security.ui.screens.ExamVaultScreen
import com.smartexambox.security.ui.screens.GeofenceConfigScreen
import com.smartexambox.security.ui.screens.LocationTrackingScreen
import com.smartexambox.security.ui.screens.LoginPinScreen
import com.smartexambox.security.ui.screens.SecurityEventLogScreen
import com.smartexambox.security.ui.screens.SettingsPermissionsScreen
import com.smartexambox.security.ui.screens.SplashScreen

@Composable
fun MainNavigation(
    viewModel: MainSecurityViewModel,
    activity: FragmentActivity
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ScreenRoutes.SPLASH
    ) {
        composable(ScreenRoutes.SPLASH) {
            SplashScreen(
                viewModel = viewModel,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(ScreenRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(ScreenRoutes.LOGIN_PIN) {
            LoginPinScreen(
                viewModel = viewModel,
                activity = activity,
                onNavigate = { route ->
                    navController.navigate(route) {
                        if (route == ScreenRoutes.DASHBOARD) {
                            popUpTo(ScreenRoutes.LOGIN_PIN) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(ScreenRoutes.BIOMETRIC) {
            BiometricAuthScreen(
                viewModel = viewModel,
                activity = activity,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(ScreenRoutes.DASHBOARD) {
            DashboardScreen(
                viewModel = viewModel,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(ScreenRoutes.VAULT) {
            ExamVaultScreen(
                viewModel = viewModel,
                activity = activity,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(ScreenRoutes.LOCATION) {
            LocationTrackingScreen(
                viewModel = viewModel,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(ScreenRoutes.EVENT_LOG) {
            SecurityEventLogScreen(
                viewModel = viewModel,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(ScreenRoutes.ALERTS) {
            AlertsNotificationScreen(
                viewModel = viewModel,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(ScreenRoutes.ADMIN) {
            AdminDashboardScreen(
                viewModel = viewModel,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(ScreenRoutes.REGISTRATION) {
            DeviceRegistrationScreen(
                viewModel = viewModel,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(ScreenRoutes.GEOFENCE) {
            GeofenceConfigScreen(
                viewModel = viewModel,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(ScreenRoutes.SETTINGS) {
            SettingsPermissionsScreen(
                viewModel = viewModel,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(ScreenRoutes.ABOUT) {
            AboutProjectScreen(
                viewModel = viewModel,
                onNavigate = { route -> navController.navigate(route) }
            )
        }
    }
}

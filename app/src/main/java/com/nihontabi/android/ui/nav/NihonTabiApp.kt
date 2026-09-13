package com.nihontabi.android.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nihontabi.android.feature.auth.LoginScreen
import com.nihontabi.android.feature.auth.RegisterScreen
import com.nihontabi.android.feature.map.MapScreen
import com.nihontabi.android.feature.map.PrefectureScreen
import com.nihontabi.android.feature.visits.VisitsListScreen

@Composable
fun NihonTabiApp(sessionViewModel: SessionViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val isLoggedIn by sessionViewModel.isLoggedIn.collectAsState()
    val startDestination = remember { if (isLoggedIn) Routes.MAP_COUNTRY else Routes.LOGIN }

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute == Routes.MAP_COUNTRY || currentRoute == Routes.VISITS

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.MAP_COUNTRY,
                        onClick = {
                            navController.navigate(Routes.MAP_COUNTRY) {
                                popUpTo(Routes.MAP_COUNTRY) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Map, contentDescription = null) },
                        label = { Text("Atlas") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.VISITS,
                        onClick = {
                            navController.navigate(Routes.VISITS) {
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Filled.List, contentDescription = null) },
                        label = { Text("Timeline") },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoggedIn = {
                        navController.navigate(Routes.MAP_COUNTRY) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegistered = {
                        navController.navigate(Routes.MAP_COUNTRY) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() },
                )
            }
            composable(Routes.MAP_COUNTRY) {
                MapScreen(onOpenPrefecture = { id -> navController.navigate(Routes.mapPrefecture(id)) })
            }
            composable(
                route = Routes.MAP_PREFECTURE,
                arguments = listOf(navArgument("prefectureId") { type = NavType.IntType }),
            ) { entry ->
                val prefectureId = entry.arguments?.getInt("prefectureId") ?: return@composable
                PrefectureScreen(prefectureId = prefectureId, onBack = { navController.popBackStack() })
            }
            composable(Routes.VISITS) {
                VisitsListScreen(onOpenPrefecture = { id -> navController.navigate(Routes.mapPrefecture(id)) })
            }
        }
    }
}

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.firebase.FirebaseAuth
import com.example.ui.navigation.Screen
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val auth = FirebaseAuth.getInstance(applicationContext)
                    val currentUser by auth.currentUser.collectAsState()

                    // Reactive Navigation Routing based on session bounds
                    val startDestination = if (currentUser == null) Screen.Auth.route else Screen.Dashboard.route

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        composable(Screen.Auth.route) {
                            AuthScreen(
                                onAuthSuccess = {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Auth.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Screen.Dashboard.route) {
                            DashboardScreen(
                                onNavigateToEditor = { projectId ->
                                    navController.navigate(Screen.Editor.createRoute(projectId))
                                },
                                onNavigateToAdmin = {
                                    navController.navigate(Screen.Admin.route)
                                },
                                onLogout = {
                                    navController.navigate(Screen.Auth.route) {
                                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(
                            route = Screen.Editor.route,
                            arguments = listOf(navArgument("projectId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val projectId = backStackEntry.arguments?.getInt("projectId") ?: 0
                            EditorScreen(
                                projectId = projectId,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(Screen.Admin.route) {
                            AdminScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

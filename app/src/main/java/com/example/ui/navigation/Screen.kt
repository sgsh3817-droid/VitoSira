package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Dashboard : Screen("dashboard")
    object Editor : Screen("editor/{projectId}") {
        fun createRoute(projectId: Int) = "editor/$projectId"
    }
    object Admin : Screen("admin")
}

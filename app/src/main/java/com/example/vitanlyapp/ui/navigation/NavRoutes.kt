package com.example.vitanlyapp.ui.navigation

/**
 * Навигационные маршруты приложения.
 */
sealed class NavRoutes(val route: String) {
    data object Onboarding : NavRoutes("onboarding")
    data object Main : NavRoutes("main")
}

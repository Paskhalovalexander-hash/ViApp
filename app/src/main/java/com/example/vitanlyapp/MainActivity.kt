package com.example.vitanlyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.vitanlyapp.ui.RootViewModel
import com.example.vitanlyapp.ui.StartupState
import com.example.vitanlyapp.ui.navigation.NavRoutes
import com.example.vitanlyapp.ui.screen.main.MainScreen
import com.example.vitanlyapp.ui.screen.onboarding.OnboardingScreen
import com.example.vitanlyapp.ui.theme.VitanlyAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val rootViewModel: RootViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Устанавливаем splash screen ДО super.onCreate()
        val splashScreen = installSplashScreen()
        
        // Держим splash screen пока идет загрузка
        splashScreen.setKeepOnScreenCondition {
            rootViewModel.startupState.value == StartupState.Loading
        }
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VitanlyAppTheme {
                VitanlyApp(rootViewModel = rootViewModel)
            }
        }
    }
}

@Composable
fun VitanlyApp(
    rootViewModel: RootViewModel
) {
    val navController = rememberNavController()
    val startupState by rootViewModel.startupState.collectAsStateWithLifecycle()

    // Splash Screen скрывает загрузку, поэтому при Loading ничего не показываем
    when (startupState) {
        StartupState.Loading -> {
            // Splash screen активен - ничего не показываем
        }
        StartupState.NeedsOnboarding, StartupState.Ready -> {
            AppNavHost(
                navController = navController,
                startDestination = if (startupState == StartupState.NeedsOnboarding) {
                    NavRoutes.Onboarding.route
                } else {
                    NavRoutes.Main.route
                },
                onOnboardingComplete = {
                    rootViewModel.onOnboardingComplete()
                    navController.navigate(NavRoutes.Main.route) {
                        popUpTo(NavRoutes.Onboarding.route) { inclusive = true }
                    }
                },
                onDataReset = {
                    rootViewModel.onDataReset()
                    navController.navigate(NavRoutes.Onboarding.route) {
                        popUpTo(NavRoutes.Main.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    onOnboardingComplete: () -> Unit,
    onDataReset: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoutes.Onboarding.route) {
            OnboardingScreen(
                onComplete = onOnboardingComplete
            )
        }
        composable(NavRoutes.Main.route) {
            MainScreen(
                onResetData = onDataReset
            )
        }
    }
}
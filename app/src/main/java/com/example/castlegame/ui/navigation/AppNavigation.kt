package com.example.castlegame.ui.navigation

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.navigation.compose.*
import com.example.castlegame.ui.auth.LoginScreen
import com.example.castlegame.ui.auth.RegisterScreen
import com.example.castlegame.ui.game.GameScreen
import com.example.castlegame.ui.game.GameViewModel
import com.example.castlegame.ui.auth.AuthViewModel
import com.example.castlegame.ui.auth.ProfileScreen

@SuppressLint("ComposableDestinationInComposeScope")
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
  gameViewModel: GameViewModel
) {
    val navController = rememberNavController()
    val user by authViewModel.user.collectAsState()

    LaunchedEffect(user) {
        if (user == null) {
            navController.navigate("login") {
                popUpTo(0)
            }
        } else {
            if (navController.currentDestination?.route != "game" &&
                navController.currentDestination?.route != "profile"
            ) {
                navController.navigate("game") {
                    popUpTo(0)
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (user == null) "login" else "game"
    ) {

        composable("login") {
            LoginScreen(
                onSuccess = { /* auth listener intézi */ },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onSuccess = { /* auth listener intézi */ },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable("game") {
        // val gameViewModel: GameViewModel = viewModel()
            GameScreen(
                viewModel = gameViewModel,
                onLogout = authViewModel::logout,
                onProfileClick = { navController.navigate("profile") }
            )
    }
        composable("profile") {
            ProfileScreen(onBack = { navController.popBackStack() })
        }
    }
}


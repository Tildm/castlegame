package com.example.castlegame.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.castlegame.ui.auth.LoginScreen
import com.example.castlegame.ui.auth.RegisterScreen
import com.example.castlegame.ui.game.GameScreen
import com.example.castlegame.ui.game.GameViewModel
import com.example.castlegame.ui.auth.AuthViewModel

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
            navController.navigate("game") {
                popUpTo(0)
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
         //   val gameViewModel: GameViewModel = viewModel()
            GameScreen(
                viewModel = gameViewModel,
                onLogout = authViewModel::logout
            )
        }
    }
}


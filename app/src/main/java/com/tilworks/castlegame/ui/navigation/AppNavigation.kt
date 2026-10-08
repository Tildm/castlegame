package com.tilworks.castlegame.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.tilworks.castlegame.ui.auth.AuthViewModel
import com.tilworks.castlegame.ui.auth.CountryGateDialog
import com.tilworks.castlegame.ui.auth.LoginScreen
import com.tilworks.castlegame.ui.auth.ProfileScreen
import com.tilworks.castlegame.ui.auth.RegisterScreen

import com.tilworks.castlegame.ui.game.GameScreen
import com.tilworks.castlegame.ui.tooltip.FaqScreen
import com.tilworks.castlegame.ui.tooltip.LegalNoticesScreen
import com.tilworks.castlegame.ui.tooltip.PrivacySummaryScreen

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun AppNavigation(
    authViewModel: AuthViewModel

) {
    val navController = rememberNavController()
    val user       by authViewModel.user.collectAsState()
    val sessionKey by authViewModel.userSessionKey.collectAsState()

    LaunchedEffect(user) {
        if (user == null) {
            navController.navigate("login") { popUpTo(0) }
        } else {
            if (navController.currentDestination?.route != "game" &&
                navController.currentDestination?.route != "profile"
            ) {
                navController.navigate("game") { popUpTo(0) }
            }
        }
    }

    NavHost(
        navController    = navController,
        startDestination = if (user == null) "login" else "game"
    ) {

        composable("login") {
            LoginScreen(
                onSuccess            = { /* auth listener handles navigation */ },
                onNavigateToRegister = { navController.navigate("register") },
                onPrivacyPolicyClick = { navController.navigate("privacy_summary") }

            )
        }

        composable("register") {
            RegisterScreen(
                onSuccess         = { /* auth listener handles navigation */ },
                onNavigateToLogin = { navController.popBackStack() },
                onPrivacyPolicyClick = { navController.navigate("privacy_summary") }
            )
        }

        composable("game") {
            key(sessionKey) {
                GameScreen(
                    viewModel      = viewModel(),
                    onLogout       = authViewModel::logout,
                    onProfileClick = { navController.navigate("profile") },
                    onFaqClick     = { navController.navigate("faq") },
                    onLegalNoticesClick = { navController.navigate("legal_notices") },
                    onPrivacySummaryClick  = { navController.navigate("privacy_summary") },
                )
                // Pops itself up the moment the user has finished a round but
                // still has no country on file; renders nothing otherwise.
                CountryGateDialog()
            }
        }

        composable("profile") {
            //ProfileScreen(onBack = { navController.popBackStack() })
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onAccountDeleted = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }  // clears the entire back stack
                    }
                }
            )
        }
        composable("faq") {
            FaqScreen(onBack = { navController.popBackStack() })
        }
        composable("legal_notices") {
            LegalNoticesScreen(onBack = { navController.popBackStack() })
        }
        composable("privacy_summary") {
            PrivacySummaryScreen(onBack = { navController.popBackStack() })
        }
    }
}




/*
package com.tilworks.castlegame.ui.navigation

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.tilworks.castlegame.ui.auth.LoginScreen
import com.tilworks.castlegame.ui.auth.RegisterScreen
import com.tilworks.castlegame.ui.game.GameScreen
import com.tilworks.castlegame.ui.game.GameViewModel
import com.tilworks.castlegame.ui.auth.AuthViewModel
import com.tilworks.castlegame.ui.auth.ProfileScreen

@SuppressLint("ComposableDestinationInComposeScope")
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
  //gameViewModel: GameViewModel
) {
    val navController = rememberNavController()
    val user by authViewModel.user.collectAsState()
    val sessionKey by authViewModel.userSessionKey.collectAsStateWithLifecycle()

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
                onSuccess = { */
/* auth listener intézi *//*
 },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onSuccess = { */
/* auth listener intézi *//*
 },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable("game") {
            // key(sessionKey) destroys and recreates everything inside whenever
            // the signed-in user changes (login, logout, or account switch).
            // GameViewModel is created with viewModel() INSIDE the block, so it
            // is scoped to this key — a new instance with fresh state is created
            // for every distinct user, and the old one has onCleared() called.
            key(sessionKey) {
                GameScreen(
                    viewModel      = viewModel(),   // fresh instance per user
                    onLogout       = authViewModel::logout,
                    onProfileClick = { navController.navigate("profile") }
                )
            }
        }
        composable("profile") {
            // ProfileViewModel is scoped to this backstack entry already,
            // so no key() needed here — it always reads the current user.
            ProfileScreen(onBack = { navController.popBackStack() })
        }
    }
}

*/

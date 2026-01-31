package com.example.castlegame

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.castlegame.ui.auth.AuthViewModel
import com.example.castlegame.ui.game.GameViewModel
import com.example.castlegame.ui.navigation.AppNavigation
//import com.example.castlegame.ui.navigation.CastleGameAppNav


class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val gameViewModel: GameViewModel = viewModel()

            AppNavigation(
                authViewModel = authViewModel,
               gameViewModel = gameViewModel
            )
        }
    }
}





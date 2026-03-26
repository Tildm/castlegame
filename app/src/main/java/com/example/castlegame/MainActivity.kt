package com.example.castlegame

import android.util.Base64
import android.util.Log
import java.security.MessageDigest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log.d
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



        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures!!) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT))
            }
        } catch (e: Exception) {
            Log.e("KeyHash:", e.toString())
        }

        setContent {
            val authViewModel: AuthViewModel = viewModel()
          // val gameViewModel: GameViewModel = viewModel()

            AppNavigation(
                authViewModel = authViewModel,
             //  gameViewModel = gameViewModel
            )
        }
    }
}





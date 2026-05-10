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
import com.google.firebase.messaging.FirebaseMessaging


//import com.example.castlegame.ui.navigation.CastleGameAppNav

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Always request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }

        // 2. Always get FCM token and subscribe
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e("FCM", "Token fetch failed", task.exception)
                return@addOnCompleteListener
            }
            Log.d("FCM_TOKEN", "Device token: ${task.result}")
        }

        FirebaseMessaging.getInstance().subscribeToTopic("uj_varak")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM-messages", "✅ Subscribed to uj_varak topic")
                } else {
                    Log.e("FCM-messages", "❌ Topic subscription failed")
                }
            }

        // 3. Check if app was opened from a notification tap
        val castleIdFromNotification = intent.extras?.getString("castleId")
        if (castleIdFromNotification != null) {
            Log.d("FCM", "App opened from notification, castleId: $castleIdFromNotification")
        }

        // 4. KeyHash (can keep for Facebook login, or remove if not needed)
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

        // 5. Always set content - app always launches correctly
        setContent {
            val authViewModel: AuthViewModel = viewModel()
            AppNavigation(
                authViewModel = authViewModel,
                // pass castleIdFromNotification here if you want to navigate on launch
            )
        }
    }
}

/*class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.extras?.let { extras ->
            val castleId = extras.getString("castleId")
            if (castleId != null) {
                Log.d("FCM", "App opened from notification, castleId: $castleId")

                // Add at the top of onCreate, after super.onCreate()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissions(
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        101
                    )
                }

// FCM TOKEN TEST - log the device token
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.e("FCM-messages", "Token fetch failed", task.exception)
                        return@addOnCompleteListener
                    }
                    val token = task.result
                    Log.d("FCM_TOKEN", "Device token: $token") // ← copy this from Logcat
                }

                // Subscribe to your topic (matches your castles.js "uj_varak" topic)
                FirebaseMessaging.getInstance().subscribeToTopic("uj_varak")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("FCM-messages", "✅ Subscribed to uj_varak topic")
                        } else {
                            Log.e("FCM-messages", "❌ Topic subscription failed")
                        }
                    }

                try {
                    val info =
                        packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
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
        }}
    }*/





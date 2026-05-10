package com.example.castlegame

import android.app.Application
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

class CastleGameApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        // Initialize Facebook SDK
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(this)

        // ✅ Enable Firestore offline persistence using the new API
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()

        FirebaseFirestore.getInstance().firestoreSettings = settings
    }
}

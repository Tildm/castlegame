package com.example.castlegame

import android.app.Application
import com.google.firebase.FirebaseApp

class CastleGameApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}

package com.example.castlegame.data.repository

import LeagueResult
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun saveLeagueResult(
        userId: String,
        result: LeagueResult,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        db.collection("users")
            .document(userId)
            .collection("leagues")
            .document(result.league.name)
            .set(result)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }
}

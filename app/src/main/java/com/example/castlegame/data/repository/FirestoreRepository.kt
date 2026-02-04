package com.example.castlegame.data.repository

import LeagueResult
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.castlegame.data.model.CastleItem
import com.example.castlegame.data.model.GlobalCastle
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions


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

    fun saveInternationalResult(
        userId: String,
        winner: CastleItem
    ) {
        val userRef = db
            .collection("users")
            .document(userId)
            .collection("international_result")
            .document("latest")

        val globalRef = db
            .collection("global_superleague")
            .document("castles")
            .collection("items")
            .document(winner.id)

        db.runBatch { batch ->
            // 👤 user eredmény
            batch.set(
                userRef,
                mapOf(
                    "winnerId" to winner.id,
                    "winnerTitle" to winner.title,
                    "imageUrl" to winner.imageUrl,
                    "timestamp" to System.currentTimeMillis()
                )
            )

            // 🌍 globális aggregáció
            batch.set(
                globalRef,
                mapOf(
                    "title" to winner.title,
                    "imageUrl" to winner.imageUrl,
                    "wins" to FieldValue.increment(1)
                ),
                SetOptions.merge()
            )
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun loadGlobalSuperLeagueRanking(
        onSuccess: (List<GlobalCastle>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        db.collection("global_superleague")
            .document("ranking")
            .collection("castles")
            .orderBy("wins", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    val wins = doc.getLong("wins") ?: return@mapNotNull null
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val imageUrl = doc.getString("imageUrl") ?: ""

                    GlobalCastle(
                        id = doc.id,
                        title = title,
                        imageUrl = imageUrl,
                        wins = wins
                    )
                }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it) }
    }
}
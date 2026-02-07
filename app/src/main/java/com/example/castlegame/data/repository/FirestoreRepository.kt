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
        limit: Int = 50,
        onSuccess: (List<GlobalCastle>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("global_superleague_ranking")
            .orderBy("wins", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(GlobalCastle::class.java)
                }
                onSuccess(list)
            }
            .addOnFailureListener(onError)
    }


    fun saveSuperLeagueResults(
        results: List<GlobalCastle>,
        onError: (Exception) -> Unit = {}
    ) {
        results.forEach { castle ->
            val ref = db
                .collection("global_superleague_ranking")
                .document(castle.id)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(ref)

                if (snapshot.exists()) {
                    transaction.update(
                        ref,
                        mapOf(
                            "wins" to FieldValue.increment(castle.wins.toLong()),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    transaction.set(
                        ref,
                        mapOf(
                            "castleId" to castle.id,
                            "title" to castle.title,
                            "imageUrl" to castle.imageUrl,
                            "wins" to castle.wins,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.addOnFailureListener {
                onError(it)
            }
        }
    }

}
package com.example.castlegame.data.repository

import LeagueResult
import android.os.Build
import android.util.Log
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
        Log.d("FirestoreRepository", "Loading global ranking from Firestore...")

        db.collection("global_superleague_ranking")
            .orderBy("wins", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d("FirestoreRepository", "Loaded ${snapshot.size()} castles from Firestore")

                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        val castle = GlobalCastle(
                            id = doc.getString("id") ?: doc.id,
                            title = doc.getString("title") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: "",
                            wins = (doc.getLong("wins") ?: 0L).toInt(),
                            description = (doc.getString("description") ?: ""),
                            wikiUrl = doc.getString("wikiUrl") ?: "",
                            country = doc.getString("country") ?: "",
                            built = doc.getString("built") ?: "",
                            style = doc.getString("style") ?: "",
                            visiting = doc.getString("visiting") ?: "",
                            location = doc.getString("location") ?: ""
                        )
                        Log.d("FirestoreRepository", "Loaded castle: ${castle.title} with ${castle.wins} wins")
                        castle
                    } catch (e: Exception) {
                        Log.e("FirestoreRepository", "Error parsing castle: ${doc.id}", e)
                        null
                    }
                }

                if (list.isEmpty()) {
                    Log.w("FirestoreRepository", "No castles found in global ranking")
                }

                onSuccess(list)
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreRepository", "Error loading global ranking", e)
                onError(e)
            }
    }

    fun saveSuperLeagueResults(
        results: List<GlobalCastle>,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        Log.d("FirestoreRepository", "Saving ${results.size} castle results to Firestore...")

        // Use batch write for better performance and offline support
        val batch = db.batch()

        results.forEach { castle ->
            val ref = db
                .collection("global_superleague_ranking")
                .document(castle.id)

            Log.d("FirestoreRepository", "Saving ${castle.title}: ${castle.wins} wins")

            // Use merge to handle both new and existing documents
            batch.set(
                ref,
                mapOf(
                    "id" to castle.id,
                    "title" to castle.title,
                    "imageUrl" to castle.imageUrl,
                    "wins" to FieldValue.increment(castle.wins.toLong()),
                    "description" to castle.description,
                    "wikiUrl" to castle.wikiUrl,
                    "country" to castle.country,
                    "built" to castle.built,
                    "style" to castle.style,
                    "visiting" to castle.visiting,
                    "location" to castle.location,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
        }

        // Commit the batch
        batch.commit()
            .addOnSuccessListener {
                Log.d("FirestoreRepository", "SuperLeague results saved successfully")
                onSuccess()
            }
            .addOnFailureListener { e ->
                // ✅ Even if offline, the write is cached and will sync later
                if (e.message?.contains("UNAVAILABLE") == true ||
                    e.message?.contains("offline") == true) {
                    Log.w("FirestoreRepository", "Saved locally, will sync when online", e)
                    onSuccess() // ✅ Still call success - it's cached!
                } else {
                    Log.e("FirestoreRepository", "Error saving results", e)
                    onError(e)
                }
            }
    }

}
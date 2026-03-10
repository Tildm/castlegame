package com.example.castlegame.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.castlegame.data.model.ApiCastle
import com.example.castlegame.data.model.GlobalCastle
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

class GlobalRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Loads the global ranking from Firestore (wins only), then enriches
     * each entry with castle details from the already-fetched [apiCastles] list.
     * This way Firestore only stores what it owns: the win counts.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun loadGlobalSuperLeagueRanking(
        apiCastles: List<ApiCastle>,
        limit: Int = 50,
        onSuccess: (List<GlobalCastle>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        Log.d("FirestoreRepository", "Loading global ranking from Firestore...")

        // Build a quick lookup map: castle id -> ApiCastle
        val apiMap = apiCastles.associateBy { it.id }

        db.collection("global_superleague_ranking")
            .orderBy("wins", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d("FirestoreRepository", "Loaded ${snapshot.size()} castles from Firestore")

                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.id
                        val wins = (doc.getLong("wins") ?: 0L).toInt()

                        // Enrich from API data; if castle was removed from API it will show blanks
                        val api = apiMap[id]

                        val castle = GlobalCastle(
                            id = id,
                            title = api?.title ?: "",
                            imageUrl = api?.image?.map { it.url } ?: emptyList(),
                            wins = wins,
                            description = api?.description ?: "",
                            wikiUrl = api?.wikiUrl ?: "",
                            country = api?.country ?: "",
                            built = api?.built ?: "",
                            style = api?.style ?: "",
                            visiting = api?.visiting ?: "",
                            location = api?.location ?: ""
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

    fun saveSuperLeagueResultsWithHistory(
        userId: String?,
        results: List<GlobalCastle>,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = now }
        val year = calendar.get(java.util.Calendar.YEAR)
        val week = calendar.get(java.util.Calendar.WEEK_OF_YEAR)
        val weekKey = "$year-W${week.toString().padStart(2, '0')}"  // e.g. "2026-W09"

        val batch = db.batch()

        Log.d("FirestoreRepository", "Saving ${results.size} castle results to Firestore...")

        // 1. Update cumulative ranking (same as before — wins only)
        results.forEach { castle ->
            val ref = db.collection("global_superleague_ranking").document(castle.id)
            batch.set(
                ref,
                mapOf(
                    "wins" to FieldValue.increment(castle.wins.toLong()),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "castleId" to castle.id,
                    "castleTitle" to castle.title,
                ),
                SetOptions.merge()
            )
        }


        // 2. Weekly cumulative ranking (increment per week)

        results.forEach { castle ->

            val weeklyRef = db
                .collection("global_superleague_weekly_ranking")
                .document(weekKey)
                .collection("castles")
                .document(castle.id)

            batch.set(
                weeklyRef,
                mapOf(
                    "wins" to FieldValue.increment(castle.wins.toLong()),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "castleId" to castle.id,
                    "castleTitle" to castle.title,
                    "week" to weekKey
                ),
                SetOptions.merge()
            )
        }

        //lekerdezes:
        /*db.collection("global_superleague_weekly_ranking")
    .document("2026-W09")
    .collection("castles")
    .orderBy("wins", Query.Direction.DESCENDING)*/



        // Set a placeholder on the week document so the subcollection path is valid
       /* val weekRef = db
            .collection("global_superleague_history")
            .document(weekKey)

        batch.set(
            weekRef,
            mapOf("week" to weekKey),
            SetOptions.merge()
        )

        // 2. Save this session as a history snapshot under the current week
        val sessionRef = db
            .collection("global_superleague_history")
            .document(weekKey)
            .collection("sessions")
            .document()  // auto-generated id

        batch.set(
            sessionRef,
            mapOf(
                "userId" to userId,
                "playedAt" to FieldValue.serverTimestamp(),
                "results" to results.map { mapOf("id" to it.id, "wins" to it.wins) }
            )
        )*/

        batch.commit()
            .addOnSuccessListener {
                Log.d("FirestoreRepository", "SuperLeague results saved successfully")
                onSuccess() }
            .addOnFailureListener { e ->
                Log.e("FirestoreRepository", "saveSuperLeagueResultsWithHistory FAILED: ${e.message}", e)
                if (e.message?.contains("UNAVAILABLE") == true ||
                    e.message?.contains("offline") == true) {
                    onSuccess()
                } else {
                    onError(e)
                }
            }
    }

    /*fun saveSuperLeagueResults(
        results: List<GlobalCastle>,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        Log.d("FirestoreRepository", "Saving ${results.size} castle results to Firestore...")

        val batch = db.batch()

        results.forEach { castle ->
            val ref = db
                .collection("global_superleague_ranking")
                .document(castle.id)

            Log.d("FirestoreRepository", "Saving ${castle.title}: ${castle.wins} wins")

            // Only store the win count — all other castle data comes from the API
            batch.set(
                ref,
                mapOf(
                    "wins" to FieldValue.increment(castle.wins.toLong())
                ),
                SetOptions.merge()
            )
        }

        batch.commit()
            .addOnSuccessListener {
                Log.d("FirestoreRepository", "SuperLeague results saved successfully")
                onSuccess()
            }
            .addOnFailureListener { e ->
                if (e.message?.contains("UNAVAILABLE") == true ||
                    e.message?.contains("offline") == true) {
                    Log.w("FirestoreRepository", "Saved locally, will sync when online", e)
                    onSuccess()
                } else {
                    Log.e("FirestoreRepository", "Error saving results", e)
                    onError(e)
                }
            }
    }*/
}

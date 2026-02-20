package com.example.castlegame.data.repository

import android.util.Log
import com.example.castlegame.data.model.GlobalCastle
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class GlobalRepository {

    private val db = FirebaseFirestore.getInstance()

    fun loadGlobalRanking(
        leagueId: String,
        onError: (Exception) -> Unit = {},
        onResult: (List<GlobalCastle>) -> Unit
    ) {
        db.collection("global_leagues")
            .document(leagueId)
            .collection("castles")
            .orderBy("wins", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull {
                    try {
                        GlobalCastle(
                            id = it.id,
                            title = it.getString("title") ?: "",
                            imageUrl = it.getString("imageUrl") ?: "",
                            wins = (it.getLong("wins") ?: 0L).toInt(),  // ← Convert Long to Int
                            description = it.getString("description") ?: "",
                            visiting = it.getString("visiting") ?: "",
                            wikiUrl = it.getString("wikiUrl") ?: "",
                            country = it.getString("country") ?: "",
                            built = it.getString("built") ?: "",
                            style = it.getString("style") ?: "",
                            location = it.getString("location") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e("GlobalRepository", "Error parsing castle: ${it.id}", e)
                        null
                    }
                }
                Log.d("GlobalRepository", "Loaded ${list.size} castles from global ranking")
                onResult(list)
            }
            .addOnFailureListener { exception ->
                Log.e("GlobalRepository", "Global ranking load failed", exception)
                onError(exception)
            }
    }
}

/*
package com.example.castlegame.data.repository

import android.util.Log
import com.example.castlegame.data.model.GlobalCastle
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class GlobalRepository {

    private val db = FirebaseFirestore.getInstance()

    fun loadGlobalRanking(
        leagueId: String,
        onError: (Exception) -> Unit = {},
        onResult: (List<GlobalCastle>) -> Unit
    ) {
        db.collection("global_leagues")
            .document(leagueId)
            .collection("castles")
            .orderBy("wins", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map {
                    GlobalCastle(
                        id = it.id,
                        title = it.getString("title") ?: "",
                        imageUrl = it.getString("imageUrl") ?: "",
                        wins = (it.getLong("wins") ?: 0L).toInt(),
                        description = it.getString("description") ?: "",
                        visiting = it.getString("visiting") ?: "",
                    )
                }
                onResult(list)
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Global ranking load failed", exception)
                onError(exception)
            }
    }
}
*/

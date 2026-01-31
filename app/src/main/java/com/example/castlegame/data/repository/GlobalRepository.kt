package com.example.castlegame.data.repository

import com.example.castlegame.data.model.GlobalCastle
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class GlobalRepository {

    private val db = FirebaseFirestore.getInstance()

    fun loadGlobalRanking(
        leagueId: String,
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
                        wins = it.getLong("wins") ?: 0
                    )
                }
                onResult(list)
            }
    }
}

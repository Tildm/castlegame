package com.example.castlegame.data.repository

import com.example.castlegame.data.model.CastleItem
import com.example.castlegame.data.model.League
import com.example.castlegame.data.remote.NetworkModule
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.UUID

class LeagueRepository {
    private val api = NetworkModule.api
    private val db = FirebaseFirestore.getInstance()

    suspend fun loadLeagues(): Map<League, List<CastleItem>> {
        val castles = try {
            api.getAllCastles()
        } catch (e: Exception) {
            emptyList()
        }

        val result = mutableMapOf(
            League.EAST to mutableListOf<CastleItem>(),
            League.WEST to mutableListOf<CastleItem>(),
            League.NORTH to mutableListOf<CastleItem>(),
            League.SOUTH to mutableListOf<CastleItem>()
        )

        for (castle in castles) {
            val item = CastleItem(
                imageUrl = castle.image.firstOrNull()?.url.orEmpty(),
                title = castle.title,
               // id = UUID.randomUUID().toString(),
                id = castle.id,
                //id = castle.title.replace(" ", "_").lowercase(), // Consistent ID based on title,
                text = castle.title // Using title as text since ApiCastle doesn't have a text field
            )

            when (castle.group) {
                "East League" -> result[League.EAST]?.add(item)
                "West League" -> result[League.WEST]?.add(item)
                "North League" -> result[League.NORTH]?.add(item)
                "South League" -> result[League.SOUTH]?.add(item)
            }
        }

        return result
    }

    fun saveLeagueResult(
        userId: String,
        leagueId: String,
        winner: CastleItem
    ) {
        val userLeagueRef = db
            .collection("users")
            .document(userId)
            .collection("leagues")
            .document(leagueId)

        val globalRef = db
            .collection("global_leagues")
            .document(leagueId)
            .collection("castles")
            .document(winner.id)

        db.runBatch { batch ->
            batch.set(
                userLeagueRef,
                mapOf(
                    "winnerId" to winner.id,
                    "winnerTitle" to winner.title,
                    "timestamp" to System.currentTimeMillis()
                )
            )

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

}


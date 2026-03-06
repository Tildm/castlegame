package com.example.castlegame.data.repository

import com.example.castlegame.data.model.CastleItem
import com.example.castlegame.data.model.League
import com.example.castlegame.data.remote.NetworkModule
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import android.util.Log
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class LeagueRepository {
    private val api = NetworkModule.api
    private val db = FirebaseFirestore.getInstance()

    suspend fun loadAllCastles(): List<CastleItem> {
        return try {
            api.getAllCastles().map { castle ->
                CastleItem(
                    imageUrl    = castle.image.map { it.url },
                    title       = castle.title,
                    id          = castle.id,
                    country     = castle.country,
                    description = castle.description,
                    wikiUrl     = castle.wikiUrl,
                    text        = castle.text,
                    built       = castle.built,
                    style       = castle.style,
                    visiting    = castle.visiting,
                    location    = castle.location,
                    group       = castle.group
                )
            }
        } catch (e: Exception) {
            Log.e("LeagueRepository", "Failed to load all castles", e)
            emptyList()
        }
    }

    suspend fun loadLeagues(): Map<League, List<CastleItem>> {

        // ── Step 1: Load all castles from the API (unchanged) ──────────────────
        val castles = try {
            api.getAllCastles()
        } catch (e: Exception) {
            emptyList()
        }

        // Build the API fallback map — same logic as before
        val apiLeagueMap = mutableMapOf(
            League.EAST  to mutableListOf<CastleItem>(),
            League.WEST  to mutableListOf<CastleItem>(),
            League.NORTH to mutableListOf<CastleItem>(),
            League.SOUTH to mutableListOf<CastleItem>()
        )

        // Also build a quick lookup: castleId → full CastleItem (for fallback filling)
        val apiCastleById = mutableMapOf<String, CastleItem>()

        for (castle in castles) {
            val item = CastleItem(
                imageUrl    = castle.image.map { it.url },
                title       = castle.title,
                id          = castle.id,
                country     = castle.country,
                description = castle.description,
                wikiUrl     = castle.wikiUrl,
                text        = castle.text,
                built       = castle.built,
                style       = castle.style,
                visiting    = castle.visiting,
                location    = castle.location,
                group       = castle.group      // ✅ make sure CastleItem has this field
            )
            apiCastleById[castle.id] = item
            when (castle.group) {
                "East League"  -> apiLeagueMap[League.EAST]?.let  { if (it.size < 6) it.add(item) }
                "West League"  -> apiLeagueMap[League.WEST]?.let  { if (it.size < 6) it.add(item) }
                "North League" -> apiLeagueMap[League.NORTH]?.let { if (it.size < 7) it.add(item) }
                "South League" -> apiLeagueMap[League.SOUTH]?.let { if (it.size < 6) it.add(item) }
            }
        }

        // ── Step 2: Get last week's key ─────────────────────────────────────────
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val week = calendar.get(Calendar.WEEK_OF_YEAR)
        // Handle week 1 edge case (previous week of previous year)
        val lastWeekKey = if (week == 1) {
            "${year - 1}-W52"
        } else {
          //  "$year-W${(week - 1).toString().padStart(2, '0')}"
            "$year-W${(week).toString().padStart(2, '0')}"
        }

        Log.d("LeagueRepository", "Loading country winners from last week: $lastWeekKey")

        // ── Step 3: Extract unique countries from API data ──────────────────────
        val countries = castles
            .map { it.country }
            .filter { it.isNotBlank() }
            .distinct()

        // ── Step 4: Read last week's winner per country from Firestore ──────────
        // winners keyed by league: League → list of winning CastleItems this week
        val firestoreLeagueMap = mutableMapOf(
            League.EAST  to mutableListOf<CastleItem>(),
            League.WEST  to mutableListOf<CastleItem>(),
            League.NORTH to mutableListOf<CastleItem>(),
            League.SOUTH to mutableListOf<CastleItem>()
        )

        for (country in countries) {
            try {
                val snapshot = db
                    .collection("country_weekly_ranking")
                    .document(lastWeekKey)
                    .collection(country)
                    .get()
                    .await()

                if (snapshot.isEmpty) {
                    Log.d("LeagueRepository", "No last week data for $country")
                    continue
                }

                // Pick the castle with most wins in this country last week
                val winnerDoc = snapshot.documents
                    .maxByOrNull { it.getLong("wins") ?: 0L }
                    ?: continue

                val castleId    = winnerDoc.getString("castleId")    ?: continue
                val group       = winnerDoc.getString("group")        ?: continue

                // Look up the full CastleItem from API data (has imageUrl, description, etc.)
                // Fall back to a minimal CastleItem if not found in API
                val fullCastle = apiCastleById[castleId] ?: CastleItem(
                    id = castleId,
                    title = winnerDoc.getString("castleTitle") ?: "",
                    country = country,
                    group = group,
                    imageUrl = emptyList(),
                    text = "",

                )

                val targetLeague = when (group) {
                    "East League"  -> League.EAST
                    "West League"  -> League.WEST
                    "North League" -> League.NORTH
                    "South League" -> League.SOUTH
                    else -> {
                        Log.w("LeagueRepository", "Unknown group '$group' for $country winner, skipping")
                        continue
                    }
                }

                firestoreLeagueMap[targetLeague]?.add(fullCastle)
                Log.d("LeagueRepository", "Country winner: ${fullCastle.title} ($country) → $targetLeague")

            } catch (e: Exception) {
                Log.w("LeagueRepository", "Failed to fetch last week data for $country: ${e.message}")
                // Non-fatal: this country just won't contribute a winner this week
            }
        }

        // ── Step 5: Merge — Firestore winners first, fill up to 6 with API ──────
        val result = mutableMapOf<League, MutableList<CastleItem>>()

        League.entries.forEach { league ->
            val firestoreWinners = firestoreLeagueMap[league] ?: mutableListOf()
            val apiCastles       = apiLeagueMap[league]       ?: mutableListOf()

            val merged = mutableListOf<CastleItem>()

            // Add Firestore country winners first (up to 6)
            merged.addAll(firestoreWinners.take(6))

            // Fill remaining slots with API castles that aren't already in the list
            val firestoreIds = merged.map { it.id }.toSet()
            for (apiCastle in apiCastles) {
                if (merged.size >= 6) break
                if (apiCastle.id !in firestoreIds) {
                    merged.add(apiCastle)
                }
            }

            result[league] = merged
            Log.d("LeagueRepository", "$league: ${firestoreWinners.size} from Firestore + ${merged.size - firestoreWinners.size} from API = ${merged.size} total")
        }

        return result
    }


    /*suspend fun loadLeagues(): Map<League, List<CastleItem>> {
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
                imageUrl = castle.image.map { it.url },
                title = castle.title,
                id = castle.id,
                country = castle.country,
                description = castle.description,
                wikiUrl = castle.wikiUrl,
                text = castle.text,
                built = castle.built,
                style = castle.style,
                visiting = castle.visiting,
                location = castle.location,
                group       = castle.group
            )
            //innen tölti be a nemzeti bajnokságokhoz a csapatokat, és a égtájak szerinti csapatokat is
            when (castle.group) {
                "East League" -> result[League.EAST]?.let { if (it.size < 6) it.add(item) }
                "West League" -> result[League.WEST]?.let { if (it.size < 6) it.add(item) }
                "North League" -> result[League.NORTH]?.let { if (it.size < 6) it.add(item) }
                "South League" -> result[League.SOUTH]?.let { if (it.size < 6) it.add(item) }
            }
        }

        return result
    }*/

    /**
     * Saves the full league ranking result:
     * 1. Cumulative wins per castle in global_leagues_ranking/{leagueId}_{castleId}
     * 2. Per-user winner record in users/{userId}/leagues/{leagueId}
     * 3. Timestamped session snapshot in global_leagues_history/{weekKey}/sessions/{auto-id}
     */
    fun saveLeagueResult(
        userId: String,
        leagueId: String,
        winner: CastleItem,
        allResults: List<Pair<CastleItem, Int>>,   // full ranking: castle → wins this session
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val week = calendar.get(Calendar.WEEK_OF_YEAR)
        val weekKey = "$year-W${week.toString().padStart(2, '0')}"

        Log.d("LeagueRepository", "Saving league $leagueId result, week $weekKey, ${allResults.size} castles")

        val batch = db.batch()

    /*    // 1. Per-user winner record - merging to avoid overwriting full LeagueResult if it exists
        val userLeagueRef = db
            .collection("users")
            .document(userId)
            .collection("leagues")
            .document(leagueId)

        batch.set(
            userLeagueRef,
            mapOf(
                "winnerId" to winner.id,
               // "winnerTitle" to winner.title,
                "lastPlayedTimestamp" to System.currentTimeMillis()
            ),
            SetOptions.merge()
        )*/

        // 2. Cumulative wins
        allResults.forEach { (castle, wins) ->
            if (wins > 0) {
                val globalRef = db
                    .collection("global_leagues_ranking")
                    .document("${leagueId}_${castle.id}")


                batch.set(
                    globalRef,
                    mapOf(
                        "wins" to FieldValue.increment(wins.toLong()),
                        "leagueId" to leagueId,
                        "castleId" to castle.id,
                        "castleTitle" to castle.title,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
            }
        }

        // 2. Weekly cumulative ranking (increment per week)
       allResults.forEach { (castle, wins) ->
        val weekRef = db
            .collection("global_leagues_weekly_ranking")
            .document(weekKey)
            .collection(leagueId)
            .document("${leagueId}_${castle.id}")


           batch.set(
               weekRef,
               mapOf(
                   "wins" to FieldValue.increment(wins.toLong()),
                   "updatedAt" to FieldValue.serverTimestamp(),
                   "castleId" to castle.id,
                   "castleTitle" to castle.title,
                   "week" to weekKey
               ),
               SetOptions.merge()
           )
       }

        batch.commit()
            .addOnSuccessListener {
                Log.d("LeagueRepository", "League $leagueId saved successfully")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("LeagueRepository", "Failed to save league $leagueId: ${e.message}", e)
                if (e.message?.contains("UNAVAILABLE") == true ||
                    e.message?.contains("offline") == true) {
                    onSuccess() // cached locally, will sync
                } else {
                    onError(e)
                }
            }
    }


/**
Saves the full league ranking result: culminate and weekly
 */
fun saveCountryResult(
    //userId: String,
    country: String,
   // winner: CastleItem,
    allResults: List<Pair<CastleItem, Int>>,
    onSuccess: () -> Unit = {},
    onError: (Exception) -> Unit = {}
) {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val week = calendar.get(Calendar.WEEK_OF_YEAR)
    val weekKey = "$year-W${week.toString().padStart(2, '0')}"

    Log.d("LeagueRepository", "Saving country $country result, week $weekKey, ${allResults.size} castles")

    val batch = db.batch()

    // 1. Cumulative wins
    allResults.forEach { (castle, wins) ->
        if (wins > 0) {
            val globalRef = db
                .collection("country_ranking")
                .document("${country}_${castle.id}")

            batch.set(
                globalRef,
                mapOf(
                    "wins"         to FieldValue.increment(wins.toLong()),
                    "country"      to country,
                    "castleId"     to castle.id,
                    "castleTitle"  to castle.title,
                    "updatedAt"    to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
        }
    }

    // 2. Weekly cumulative ranking
    allResults.forEach { (castle, wins) ->
        val weekRef = db
            .collection("country_weekly_ranking")
            .document(weekKey)
            .collection(country)
            .document("${country}_${castle.id}")

        batch.set(
            weekRef,
            mapOf(
                "wins"        to FieldValue.increment(wins.toLong()),
                "updatedAt"   to FieldValue.serverTimestamp(),
                "castleId"    to castle.id,
                "castleTitle" to castle.title,
                "country"     to country,
                "week"        to weekKey,
                "group"       to castle.group,
            ),
            SetOptions.merge()
        )
    }

    batch.commit()
        .addOnSuccessListener {
            Log.d("LeagueRepository", "Country $country saved successfully")
            onSuccess()
        }
        .addOnFailureListener { e ->
            Log.e("LeagueRepository", "Failed to save country $country: ${e.message}", e)
            if (e.message?.contains("UNAVAILABLE") == true ||
                e.message?.contains("offline") == true) {
                onSuccess() // cached locally, will sync
            } else {
                onError(e)
            }
        }
}

    /**
     * Reads country_weekly_ranking for [weekKey], finds the #1 castle per country,
     * then writes that winner into global_leagues_weekly_ranking under their group's league.
     *
     * Call this once per week — e.g. on app start, gated by a "already promoted this week" flag.
     *
     * Flow:
     *   country_weekly_ranking/{weekKey}/{country}/{country}_{castleId}
     *       → winner (max wins) per country
     *       → group field ("East League" / "West League" / …)
     *       → global_leagues_weekly_ranking/{weekKey}/{leagueId}/{leagueId}_{castleId}
     */
    fun promoteCountryWinnersToLeagues(
        weekKey: String,
        countries: List<String>,           // all countries known to the app
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        Log.d("LeagueRepository", "Promoting country winners for week $weekKey, countries=${countries.size}")

        // We need to fetch all countries' weekly data first, then batch-write promotions.
        // Use a counter to know when all async reads are done.
        val allWinners = mutableListOf<Map<String, Any>>()  // collected winner docs
        var remaining = countries.size

        if (remaining == 0) {
            onSuccess()
            return
        }

        fun onAllFetched() {
            if (allWinners.isEmpty()) {
                Log.d("LeagueRepository", "No country winners to promote")
                onSuccess()
                return
            }

            val batch = db.batch()

            allWinners.forEach { winner ->
                val group     = winner["group"]       as? String ?: return@forEach
                val castleId  = winner["castleId"]    as? String ?: return@forEach
                val castleTitle = winner["castleTitle"] as? String ?: ""
                val wins      = (winner["wins"]       as? Long)   ?: 0L
                val country   = winner["country"]     as? String ?: ""

                // Map "East League" → "EAST", etc. to match existing leagueId convention
                val leagueId = when (group) {
                    "East League"  -> "EAST"
                    "West League"  -> "WEST"
                    "North League" -> "NORTH"
                    "South League" -> "SOUTH"
                    else -> return@forEach   // unknown group — skip
                }

                val destRef = db
                    .collection("global_leagues_weekly_ranking")
                    .document(weekKey)
                    .collection(leagueId)
                    .document("${leagueId}_${castleId}")

                // Increment so multiple country winners can accumulate in the same league slot
                batch.set(
                    destRef,
                    mapOf(
                        "wins"            to FieldValue.increment(wins),
                        "castleId"        to castleId,
                        "castleTitle"     to castleTitle,
                        "country"         to country,
                        "promotedFrom"    to "country_weekly_ranking",  // audit trail
                        "week"            to weekKey,
                        "updatedAt"       to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )

                Log.d("LeagueRepository", "Promoting $castleTitle ($country) → $leagueId with $wins wins")
            }

            batch.commit()
                .addOnSuccessListener {
                    Log.d("LeagueRepository", "Promotion batch committed for week $weekKey")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e("LeagueRepository", "Promotion batch failed: ${e.message}", e)
                    onError(e)
                }
        }

        countries.forEach { country ->
            db.collection("country_weekly_ranking")
                .document(weekKey)
                .collection(country)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.isEmpty) {
                        // Find the castle with the most wins in this country this week
                        val winner = snapshot.documents
                            .maxByOrNull { (it.getLong("wins") ?: 0L) }

                        winner?.data?.let { allWinners.add(it) }
                    }

                    remaining--
                    if (remaining == 0) onAllFetched()
                }
                .addOnFailureListener { e ->
                    Log.w("LeagueRepository", "Failed to fetch weekly data for $country: ${e.message}")
                    remaining--
                    if (remaining == 0) onAllFetched()
                }
        }
    }


}
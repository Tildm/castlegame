import com.example.castlegame.data.model.League

data class LeagueResult(
    val league: League,
    val scores: Map<String, Int>,   // castleId -> pont
    val ranking: List<String>,      // castleId sorrend
    val finishedAt: Long = System.currentTimeMillis()
)

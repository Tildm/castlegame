data class UserGame(
    val userId: String,
    val season: Int,
    val leagues: Map<String, LeagueResult>, // "EAST" -> LeagueResult
    val superLeagueFinished: Boolean = false
)

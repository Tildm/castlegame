data class CastlesResponse(
    val castles: List<ApiCastle>
)

data class ApiCastle(
    val title: String,
    val group: String,
    val image: List<ApiImage>
)

data class ApiImage(
    val url: String
)

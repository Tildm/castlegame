package com.tilworks.castlegame.data.model

import com.squareup.moshi.Json



data class ApiCastle(
    @Json(name = "_id")  // ✅ Use Moshi's @Json annotation
    val id: String,
    val title: String,
    val group: String,
    val image: List<ApiImage>,
    val description: String,
    val wikiUrl: String = "",
    val country: String = "",
    val text: String = "",
    val built: String = "",
    val style: String = "",
    val visiting: String = "",
    val location: String = ""
)


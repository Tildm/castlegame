package com.example.castlegame.data.model

import com.squareup.moshi.Json



data class ApiCastle(
    @Json(name = "_id")  // ✅ Use Moshi's @Json annotation
    val id: String,
    val title: String,
    val group: String,
    val image: List<ApiImage>,
)


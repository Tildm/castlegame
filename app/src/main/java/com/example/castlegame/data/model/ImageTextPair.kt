package com.example.castlegame.data.model

data class CastleItem(
    val imageUrl: String,
    val title: String,
    val id: String,
    val text: String,
    val wikiUrl: String = "",

    val country: String = "",
    val built: String = "",
    val style: String = "",
    val description: String = "",
    val visiting: String = "",
    val location: String = ""
)


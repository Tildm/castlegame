package com.example.castlegame.data.model

data class CastleItem(
    val imageUrl: String,
    val title: String,
   // val fallbackImageRes: Int? = null,
    val id: String,
    val text: String,
    val wikiUrl: String = ""
)



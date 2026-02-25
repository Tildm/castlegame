package com.example.castlegame.data.model

data class GlobalCastle(
    val id: String = "",
    val title: String = "",
    //val imageUrl: String = "",
    val imageUrl: List<String>,
    val wins: Int = 0,
    val description: String,
    val wikiUrl: String = "",
    val country: String = "",
    val built: String = "",
    val style: String = "",
    val visiting: String = "",
    val location: String = ""
    )


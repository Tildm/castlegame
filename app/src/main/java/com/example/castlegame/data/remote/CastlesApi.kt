package com.example.castlegame.data.remote

import com.example.castlegame.data.model.ApiCastle
import retrofit2.http.GET

interface CastlesApi {

    @GET("all")
    suspend fun getAllCastles(): List<ApiCastle>
}







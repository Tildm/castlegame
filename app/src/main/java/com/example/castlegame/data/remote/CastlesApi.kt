package com.example.castlegame.data.remote

import com.example.castlegame.data.model.ApiCastle
import com.example.castlegame.ui.tooltip.TooltipInfo
import retrofit2.http.GET
import retrofit2.http.Path

interface CastlesApi {

    @GET("all")
    suspend fun getAllCastles(): List<ApiCastle>

    @GET("infos/{id}/json")
    suspend fun getInfoTooltips(
        @Path("id") id: String
    ): TooltipInfo
}







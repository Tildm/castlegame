package com.tilworks.castlegame.data.remote

import com.tilworks.castlegame.data.model.ApiCastle
import com.tilworks.castlegame.data.model.QuizDto
import com.tilworks.castlegame.ui.tooltip.TooltipInfo
import retrofit2.http.GET
import retrofit2.http.Path

interface CastlesApi {

    @GET("all")
    suspend fun getAllCastles(): List<ApiCastle>

    @GET("allQuizzes")
    suspend fun getAllQuizzes(): List<QuizDto>

    @GET("infos/{id}/json")
    suspend fun getInfoTooltips(
        @Path("id") id: String,
    ): TooltipInfo
}







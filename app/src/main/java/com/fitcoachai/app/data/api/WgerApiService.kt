package com.fitcoachai.app.data.api

import com.fitcoachai.app.data.model.ExerciseDetailResponse
import com.fitcoachai.app.data.model.ExerciseInfoListResponse
import com.fitcoachai.app.data.model.ExerciseListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WgerApiService {

    @GET("exerciseinfo/")
    suspend fun getExercisesWithInfo(
        @Query("format") format: String = "json",
        @Query("language") language: Int = 2,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): ExerciseInfoListResponse

    @GET("exerciseinfo/")
    suspend fun searchExercisesByCategory(
        @Query("format") format: String = "json",
        @Query("language") language: Int = 2,
        @Query("limit") limit: Int = 20,
        @Query("category") category: Int = 0
    ): ExerciseInfoListResponse

    // ✅ Détail exercice avec images
    @GET("exerciseinfo/{id}/")
    suspend fun getExerciseDetail(
        @Path("id") id: Int,
        @Query("format") format: String = "json"
    ): ExerciseDetailResponse
}
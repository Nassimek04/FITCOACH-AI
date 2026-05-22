package com.fitcoachai.app.data.api

import com.fitcoachai.app.data.model.remote.*
import retrofit2.Response
import retrofit2.http.*

interface FitCoachApiService {

    // --- AUTH ---
    @POST("auth/register")
    suspend fun register(@Body data: BackendUserCreate): Response<BackendToken>

    @POST("auth/login")
    suspend fun login(@Body data: BackendUserLogin): Response<BackendToken>

    @GET("auth/me")
    suspend fun getMe(@Header("Authorization") token: String): Response<BackendUser>

    // --- WORKOUTS ---
    @GET("workouts/")
    suspend fun getWorkouts(@Header("Authorization") token: String): Response<List<BackendWorkoutResponse>>

    @POST("workouts/")
    suspend fun createWorkout(
        @Header("Authorization") token: String,
        @Body data: BackendWorkoutCreate
    ): Response<BackendWorkoutResponse>

    // --- RUNS ---
    @GET("runs/")
    suspend fun getRuns(@Header("Authorization") token: String): Response<List<BackendRunResponse>>

    @POST("runs/")
    suspend fun createRun(
        @Header("Authorization") token: String,
        @Body data: BackendRunCreate
    ): Response<Any>

    // --- CHAT ---
    @POST("chat/")
    suspend fun chat(
        @Header("Authorization") token: String,
        @Body message: Map<String, String>
    ): Response<Map<String, String>>
}

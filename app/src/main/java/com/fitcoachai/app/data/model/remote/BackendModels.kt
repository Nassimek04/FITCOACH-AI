package com.fitcoachai.app.data.model.remote

import com.google.gson.annotations.SerializedName

data class BackendUser(
    val id: Int,
    val email: String,
    val username: String,
    @SerializedName("full_name") val fullName: String?,
    val age: Int?,
    val weight: Float?,
    val height: Float?,
    @SerializedName("fitness_goal") val fitnessGoal: String?,
    @SerializedName("created_at") val createdAt: String
)

data class BackendToken(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    val user: BackendUser
)

data class BackendUserCreate(
    val email: String,
    val username: String,
    val password: String,
    @SerializedName("full_name") val fullName: String? = null,
    val age: Int? = null,
    val weight: Float? = null,
    val height: Float? = null,
    @SerializedName("fitness_goal") val fitnessGoal: String? = null
)

data class BackendUserLogin(
    val email: String,
    val password: String
)

data class BackendWorkoutCreate(
    val name: String,
    val description: String? = null,
    val difficulty: String = "beginner",
    @SerializedName("duration_minutes") val durationMinutes: Int,
    val category: String,
    val exercises: List<Any>? = emptyList()
)

data class BackendWorkoutResponse(
    val id: Int,
    val name: String,
    val description: String?,
    val difficulty: String,
    @SerializedName("duration_minutes") val durationMinutes: Int,
    val category: String,
    val exercises: List<Any>?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("owner_id") val ownerId: Int
)

data class BackendRunCreate(
    @SerializedName("distance_km") val distanceKm: Float,
    @SerializedName("duration_seconds") val durationSeconds: Long,
    @SerializedName("avg_speed_kmh") val avgSpeedKmh: Float,
    @SerializedName("calories_burned") val caloriesBurned: Float,
    @SerializedName("route_points") val routePoints: List<Map<String, Double>> = emptyList()
)

data class BackendRunResponse(
    val id: Int,
    @SerializedName("distance_km") val distanceKm: Float,
    @SerializedName("duration_seconds") val durationSeconds: Int,
    @SerializedName("avg_speed_kmh") val avgSpeedKmh: Float,
    @SerializedName("max_speed_kmh") val maxSpeedKmh: Float,
    @SerializedName("calories_burned") val caloriesBurned: Float,
    @SerializedName("route_points") val routePoints: List<Any>?,
    val weather: String?,
    val temperature: Float?,
    @SerializedName("completed_at") val completedAt: String
)

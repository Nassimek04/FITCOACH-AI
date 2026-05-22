package com.fitcoachai.app.data.model

data class RunSession(
    val id: String = "",
    val date: Long = 0L,
    val distanceMeters: Float = 0f,
    val durationSeconds: Long = 0L,
    val calories: Int = 0,
    val speedKmh: Float = 0f
)
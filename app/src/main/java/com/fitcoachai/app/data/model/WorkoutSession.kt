package com.fitcoachai.app.data.model

data class WorkoutSession(
    val id: String = "",
    val date: Long = 0L,
    val durationSeconds: Long = 0L,
    val totalCalories: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val exercises: List<PerformedExercise> = emptyList()
)

data class PerformedExercise(
    val exerciseId: Int = 0,
    val name: String = "",
    val sets: List<ExerciseSet> = emptyList()
)

data class ExerciseSet(
    val weightKg: Double = 0.0,
    val reps: Int = 0,
    val timestamp: Long = 0L
)

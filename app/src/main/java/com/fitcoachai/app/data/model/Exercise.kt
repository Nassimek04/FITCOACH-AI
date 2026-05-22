package com.fitcoachai.app.data.model

import java.io.Serializable

// ✅ RAW SOURCE 1 (GitHub DB)
data class RawExercise(
    val name: String = "",
    val force: String? = null,
    val level: String = "",
    val mechanic: String? = null,
    val equipment: String? = null,
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val category: String = "",
    val images: List<String> = emptyList(),
    val id: String = ""
) : Serializable

// ✅ RAW SOURCE 2 (Wger API Responses)
data class ExerciseInfoListResponse(
    val results: List<ExerciseDetailResponse> = emptyList()
)

data class ExerciseListResponse(
    val results: List<WgerExerciseBase> = emptyList()
)

data class WgerExerciseBase(
    val id: Int,
    val name: String,
    val description: String = ""
)

data class ExerciseDetailResponse(
    val id: Int = 0,
    val name: String = "",
    val description: String = "",
    val category: WgerCategory? = null,
    val muscles: List<WgerMuscle> = emptyList(),
    val images: List<WgerImage> = emptyList(),
    val translations: List<WgerTranslation> = emptyList()
) : Serializable

data class WgerCategory(val id: Int, val name: String) : Serializable
data class WgerMuscle(val id: Int, val name: String, val image_url_main: String? = null) : Serializable
data class WgerImage(val id: Int, val image: String) : Serializable
data class WgerTranslation(val id: Int, val language: Int, val name: String, val description: String) : Serializable

// ✅ UNIFIED ARSENAL MODEL V14
data class ExerciseResult(
    val id: Int = 0,
    val uuid: String = "",
    val name: String = "",
    val category: String = "",
    val muscle: String = "",
    val imageUrl: String? = null,
    val description: String = "",
    val instructions: List<String> = emptyList(),
    val level: String = "intermediate",
    val equipment: String? = null,
    val force: String? = null,
    val secondaryMuscles: List<String> = emptyList(),
    val safetyTips: String = "Gardez le dos droit et contrôlez le mouvement."
) : Serializable

data class WorkoutExercise(
    val id: Int = 0,
    val name: String = "",
    val category: String = "",
    val muscle: String = "",
    val sets: Int = 3,
    val reps: Int = 12,
    val calories: Int = 0,
    val imageUrl: String? = null,
    val equipment: String? = null
)

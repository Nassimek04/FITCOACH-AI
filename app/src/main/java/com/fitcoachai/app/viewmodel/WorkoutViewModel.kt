package com.fitcoachai.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoachai.app.data.api.RetrofitClient
import com.fitcoachai.app.data.model.WorkoutExercise
import com.fitcoachai.app.data.model.WorkoutSession
import com.fitcoachai.app.data.model.PerformedExercise
import com.fitcoachai.app.data.model.ExerciseSet
import com.fitcoachai.app.data.model.remote.BackendWorkoutCreate
import com.fitcoachai.app.utils.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val sessionManager = SessionManager(application)

    private val _program = MutableStateFlow<List<WorkoutExercise>>(emptyList())
    val program: StateFlow<List<WorkoutExercise>> = _program

    private val _totalCalories = MutableStateFlow(0)
    val totalCalories: StateFlow<Int> = _totalCalories

    private val _addedMessage = MutableStateFlow("")
    val addedMessage: StateFlow<String> = _addedMessage

    // ✅ Session Tracking State
    private val _currentSession = MutableStateFlow<WorkoutSession?>(null)
    val currentSession: StateFlow<WorkoutSession?> = _currentSession

    private var sessionStartTime: Long = 0

    init {
        loadProgram()
    }

    // Calcul calories selon catégorie
    private fun calculateCalories(category: String, sets: Int, reps: Int): Int {
        val caloriesPerRep = when {
            category.contains("cardio", ignoreCase = true) -> 1.0
            category.contains("chest", ignoreCase = true) -> 0.5
            category.contains("back", ignoreCase = true) -> 0.6
            category.contains("legs", ignoreCase = true) -> 0.8
            category.contains("shoulders", ignoreCase = true) -> 0.4
            category.contains("arms", ignoreCase = true) -> 0.3
            category.contains("core", ignoreCase = true) -> 0.4
            else -> 0.5
        }
        return (sets * reps * caloriesPerRep).toInt()
    }

    fun startWorkout() {
        sessionStartTime = System.currentTimeMillis()
        _currentSession.value = WorkoutSession(
            id = UUID.randomUUID().toString(),
            date = sessionStartTime,
            exercises = _program.value.map { PerformedExercise(exerciseId = it.id, name = it.name) }
        )
    }

    fun logSet(exerciseId: Int, weight: Double, reps: Int) {
        val session = _currentSession.value ?: return
        val updatedExercises = session.exercises.map { perfEx ->
            if (perfEx.exerciseId == exerciseId) {
                perfEx.copy(sets = perfEx.sets + ExerciseSet(weight, reps, System.currentTimeMillis()))
            } else perfEx
        }
        
        val totalVolume = updatedExercises.sumOf { ex -> ex.sets.sumOf { s -> s.weightKg * s.reps } }
        _currentSession.value = session.copy(exercises = updatedExercises, totalVolumeKg = totalVolume)
    }

    fun finishWorkout() {
        val session = _currentSession.value ?: return
        val uid = auth.currentUser?.uid ?: return
        
        val duration = (System.currentTimeMillis() - sessionStartTime) / 1000
        val finalSession = session.copy(
            durationSeconds = duration,
            totalCalories = (session.totalVolumeKg * 0.1).toInt() + (duration / 60).toInt() * 5 // Rough estimate: 0.1 cal per kg moved + 5 cal per min
        )

        viewModelScope.launch {
            try {
                // 1. Firebase Save
                firestore.collection("users")
                    .document(uid)
                    .collection("workout_sessions")
                    .document(finalSession.id)
                    .set(finalSession)
                
                // 2. FastAPI Backend Save (Parallel)
                try {
                    val authHeader = sessionManager.getAuthHeader()
                    android.util.Log.d("FitCoach", "Token: $authHeader")
                    if (authHeader != null) {
                        val backendWorkout = BackendWorkoutCreate(
                            name = "Séance du ${System.currentTimeMillis()}",
                            description = "Volume: ${finalSession.totalVolumeKg}kg",
                            durationMinutes = (duration / 60).toInt(),
                            category = "Mixed",
                            exercises = finalSession.exercises.map { mapOf("id" to it.exerciseId, "name" to it.name) }
                        )
                        RetrofitClient.fitCoachApi.createWorkout(authHeader, backendWorkout)
                        android.util.Log.d("FitCoach", "Workout FastAPI success")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FitCoach", "Workout FastAPI error: ${e.message}")
                    e.printStackTrace()
                }

                _currentSession.value = null
                _addedMessage.value = "🏋️ Séance enregistrée ! Volume: ${finalSession.totalVolumeKg}kg"
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addExercise(
        id: Int,
        name: String,
        category: String,
        muscle: String,
        imageUrl: String? = null
    ) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val calories = calculateCalories(category, 3, 12)

            val exercise = WorkoutExercise(
                id = id,
                name = name,
                category = category,
                muscle = muscle,
                sets = 3,
                reps = 12,
                calories = calories,
                imageUrl = imageUrl
            )

            // Vérifie si déjà dans le programme
            val exists = _program.value.any { it.id == id }
            if (exists) {
                _addedMessage.value = "Déjà dans le programme !"
                return@launch
            }

            val newList = _program.value + exercise
            _program.value = newList
            updateTotalCalories()

            // 1. Firebase Save
            firestore.collection("users")
                .document(uid)
                .collection("program")
                .document(id.toString())
                .set(exercise)

            // 2. FastAPI Backend Sync
            try {
                val authHeader = sessionManager.getAuthHeader()
                if (authHeader != null) {
                    val backendWorkout = BackendWorkoutCreate(
                        name = "Focus: $name",
                        description = "Exercice ajouté via programme",
                        durationMinutes = 0,
                        category = category,
                        exercises = listOf(mapOf("id" to id, "name" to name))
                    )
                    android.util.Log.d("FitCoach", "addExercise: calling FastAPI createWorkout for $name")
                    val response = RetrofitClient.fitCoachApi.createWorkout(authHeader, backendWorkout)
                    if (response.isSuccessful) {
                        android.util.Log.d("FitCoach", "addExercise: FastAPI workout created successfully")
                    } else {
                        android.util.Log.e("FitCoach", "addExercise: FastAPI error: ${response.message()}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FitCoach", "addExercise: FastAPI error: ${e.message}")
            }

            _addedMessage.value = "✅ ${name} ajouté ! (~${calories} cal)"
        }
    }

    fun removeExercise(id: Int) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            _program.value = _program.value.filter { it.id != id }
            updateTotalCalories()
            firestore.collection("users")
                .document(uid)
                .collection("program")
                .document(id.toString())
                .delete()
        }
    }

    private fun updateTotalCalories() {
        _totalCalories.value = _program.value.sumOf { it.calories }
    }

    fun loadProgram() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(uid)
            .collection("program")
            .get()
            .addOnSuccessListener { docs ->
                val exercises = docs.mapNotNull { doc ->
                    doc.toObject(WorkoutExercise::class.java)
                }
                _program.value = exercises
                updateTotalCalories()
            }
    }

    fun resetMessage() {
        _addedMessage.value = ""
    }
}
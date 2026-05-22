package com.fitcoachai.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fitcoachai.app.data.model.ExerciseResult

class NavigationViewModel : ViewModel() {

    var currentScreen by mutableStateOf("splash")

    var selectedExercise by mutableStateOf<ExerciseResult?>(null)

    fun navigate(screen: String) {
        currentScreen = screen
    }

    fun selectExercise(exercise: ExerciseResult) {
        selectedExercise = exercise
        currentScreen = "exercise_detail"
    }

    fun goBack() {
        if (currentScreen == "sessions" || currentScreen == "exercise_detail") {
            currentScreen = "home"
        } else {
            selectedExercise = null
            currentScreen = "home"
        }
    }

    fun logout() {
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        selectedExercise = null
        currentScreen = "login"
    }
}

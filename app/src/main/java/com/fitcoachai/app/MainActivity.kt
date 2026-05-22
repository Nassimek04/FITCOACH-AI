package com.fitcoachai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.fitcoachai.app.ui.screens.*
import com.fitcoachai.app.ui.theme.FitCoachAITheme
import com.fitcoachai.app.viewmodel.NavigationViewModel

class MainActivity : ComponentActivity() {

    private val nav: NavigationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FitCoachAITheme {
                val currentScreen = nav.currentScreen
                val selectedExercise = nav.selectedExercise

                // ✅ Gestion du bouton retour système
                if (currentScreen == "exercise_detail" || currentScreen == "sessions") {
                    BackHandler {
                        nav.goBack()
                    }
                }

                when (currentScreen) {
                    "splash" -> SplashScreen(
                        onSplashFinished = { isLoggedIn ->
                            if (isLoggedIn) {
                                nav.navigate("home")
                            } else {
                                nav.navigate("login")
                            }
                        }
                    )
                    "login" -> LoginScreen(
                        onLoginSuccess = { nav.navigate("home") },
                        onNavigateToRegister = { nav.navigate("register") }
                    )
                    "register" -> RegisterScreen(
                        onRegisterSuccess = { nav.navigate("login") },
                        onNavigateToLogin = { nav.navigate("login") }
                    )
                    "home" -> HomeScreen(
                        onLogout = { nav.logout() },
                        onExerciseClick = { exercise ->
                            nav.selectExercise(exercise)
                        },
                        onShowSessions = { nav.navigate("sessions") }
                    )
                    "exercise_detail" -> {
                        selectedExercise?.let { exercise ->
                            ExerciseDetailScreen(
                                exercise = exercise,
                                onBack = { nav.goBack() },
                                onAddedToProgram = { nav.goBack() }
                            )
                        } ?: nav.goBack()
                    }
                    "sessions" -> SessionsScreen(
                        onBack = { nav.goBack() }
                    )
                }
            }
        }
    }
}

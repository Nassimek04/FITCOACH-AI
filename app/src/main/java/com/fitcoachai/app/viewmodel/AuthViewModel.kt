package com.fitcoachai.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoachai.app.data.api.RetrofitClient
import com.fitcoachai.app.data.model.remote.BackendUserCreate
import com.fitcoachai.app.data.model.remote.BackendUserLogin
import com.fitcoachai.app.utils.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val uid: String = "",
    val nom: String = "",
    val email: String = "",
    val age: String = "",
    val poids: String = "",
    val objectif: String = ""
)

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val sessionManager = SessionManager(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private fun validatePassword(password: String): String? {
        return when {
            password.length < 8 ->
                "Le mot de passe doit contenir au moins 8 caractères"
            !password.any { it.isUpperCase() } ->
                "Le mot de passe doit contenir au moins une majuscule"
            !password.any { it.isLowerCase() } ->
                "Le mot de passe doit contenir au moins une minuscule"
            !password.any { it.isDigit() } ->
                "Le mot de passe doit contenir au moins un chiffre"
            !password.any { "!@#\$%^&*()_+-=[]{}|;:,.<>?".contains(it) } ->
                "Le mot de passe doit contenir au moins un caractère spécial (!@#\$%...)"
            else -> null
        }
    }

    fun login(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Remplis tous les champs")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // 1. Firebase Login
                auth.signInWithEmailAndPassword(email.trim(), password).await()
                android.util.Log.d("FitCoach", "Firebase login success, calling FastAPI...")

                // 2. FastAPI Backend Login (Parallel & Automatic)
                try {
                    val loginData = BackendUserLogin(email.trim(), password)
                    val response = RetrofitClient.fitCoachApi.login(loginData)
                    if (response.isSuccessful) {
                        val tokenData = response.body()
                        if (tokenData != null) {
                            android.util.Log.d("FitCoach", "FastAPI login success: ${tokenData.accessToken}")
                            sessionManager.saveToken(tokenData.accessToken)
                            sessionManager.saveUser(tokenData.user.id, tokenData.user.email)
                        }
                    } else {
                        android.util.Log.e("FitCoach", "FastAPI login failed: Code ${response.code()} - ${response.message()}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FitCoach", "FastAPI login failed: ${e.message}")
                    // Silent fail for backend to keep Firebase working
                    e.printStackTrace()
                }

                _authState.value = AuthState.Success
            } catch (e: Exception) {
                val errorCode = e.message ?: ""
                val message = when {
                    errorCode.contains("INVALID_LOGIN_CREDENTIALS") ||
                            errorCode.contains("INVALID_PASSWORD") ||
                            errorCode.contains("wrong-password") ->
                        "Mot de passe incorrect"
                    errorCode.contains("USER_NOT_FOUND") ||
                            errorCode.contains("no user record") ->
                        "Aucun compte trouvé avec cet email"
                    errorCode.contains("INVALID_EMAIL") ||
                            errorCode.contains("badly formatted") ->
                        "Adresse email invalide"
                    errorCode.contains("network") ||
                            errorCode.contains("NETWORK") ->
                        "Erreur réseau, vérifie ta connexion"
                    errorCode.contains("too-many-requests") ->
                        "Trop de tentatives, réessaie plus tard"
                    else -> "Email ou mot de passe incorrect"
                }
                _authState.value = AuthState.Error(message)
            }
        }
    }

    fun register(
        nom: String,
        email: String,
        password: String,
        age: String,
        poids: String,
        objectif: String
    ) {
        if (nom.isEmpty() || email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Remplis tous les champs obligatoires")
            return
        }

        val passwordError = validatePassword(password)
        if (passwordError != null) {
            _authState.value = AuthState.Error(passwordError)
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // 1. Firebase Register
                val result = auth.createUserWithEmailAndPassword(
                    email.trim(), password
                ).await()
                val uid = result.user?.uid ?: return@launch

                val userProfile = UserProfile(
                    uid = uid,
                    nom = nom,
                    email = email.trim(),
                    age = age,
                    poids = poids,
                    objectif = objectif
                )

                firestore.collection("users")
                    .document(uid)
                    .set(userProfile)
                    .await()

                // 2. FastAPI Backend Register (Parallel & Automatic)
                try {
                    val backendUser = BackendUserCreate(
                        email = email.trim(),
                        username = nom.filter { !it.isWhitespace() }.lowercase(),
                        password = password,
                        fullName = nom,
                        age = age.toIntOrNull(),
                        weight = poids.toFloatOrNull(),
                        fitnessGoal = objectif
                    )
                    val response = RetrofitClient.fitCoachApi.register(backendUser)
                    if (response.isSuccessful) {
                        val tokenData = response.body()
                        if (tokenData != null) {
                            sessionManager.saveToken(tokenData.accessToken)
                            sessionManager.saveUser(tokenData.user.id, tokenData.user.email)
                        }
                    }
                } catch (e: Exception) {
                    // Silent fail for backend to keep Firebase working
                    e.printStackTrace()
                }

                _authState.value = AuthState.Success
            } catch (e: Exception) {
                val errorCode = e.message ?: ""
                val message = when {
                    errorCode.contains("already in use") ->
                        "Cet email est déjà utilisé"
                    errorCode.contains("INVALID_EMAIL") ||
                            errorCode.contains("badly formatted") ->
                        "Adresse email invalide"
                    errorCode.contains("network") ||
                            errorCode.contains("NETWORK") ->
                        "Erreur réseau, vérifie ta connexion"
                    errorCode.contains("weak-password") ->
                        "Mot de passe trop faible"
                    else -> "Erreur d'inscription"
                }
                _authState.value = AuthState.Error(message)
            }
        }
    }

    fun logout() {
        auth.signOut()
        sessionManager.clear()
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
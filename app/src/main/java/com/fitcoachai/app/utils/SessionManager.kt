package com.fitcoachai.app.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fitcoach_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TOKEN_KEY = "jwt_token"
        private const val USER_ID_KEY = "user_id"
        private const val USER_EMAIL_KEY = "user_email"
    }

    fun saveToken(token: String) {
        prefs.edit().putString(TOKEN_KEY, token).apply()
    }

    fun getToken(): String? {
        return prefs.getString(TOKEN_KEY, null)
    }

    fun saveUser(id: Int, email: String) {
        prefs.edit().apply {
            putInt(USER_ID_KEY, id)
            putString(USER_EMAIL_KEY, email)
        }.apply()
    }

    fun getUserId(): Int {
        return prefs.getInt(USER_ID_KEY, -1)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun getAuthHeader(): String? {
        val token = getToken()
        return if (token != null) "Bearer $token" else null
    }
}

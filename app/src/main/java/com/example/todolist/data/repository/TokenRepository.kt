package com.example.todolist.data.repository

import android.content.SharedPreferences
import android.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenRepository @Inject constructor(
    private val prefs: SharedPreferences
) {
    companion object {
        private const val KEY_TOKEN = "jwt_token"
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    fun hasToken(): Boolean = getToken() != null

    fun getLogin(): String {
        return try {
            val token = getToken() ?: return ""
            val payload = token.split(".")[1]
            val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING)
            val json = String(decoded)
            Regex(""""login"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.get(1) ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}

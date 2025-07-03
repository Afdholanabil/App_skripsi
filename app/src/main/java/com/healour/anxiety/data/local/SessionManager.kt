package com.healour.anxiety.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Inisialisasi DataStore
private val Context.dataStore by preferencesDataStore(name = "user_session")

class SessionManager(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("session_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val EXPIRES_AT_KEY = longPreferencesKey("expires_at")
    }

    // Simpan session user
    suspend fun saveSession(token: String, userId: String, expiresAt: Long) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USER_ID_KEY] = userId
            preferences[EXPIRES_AT_KEY] = expiresAt
        }
        android.util.Log.d("SessionManager", "Session saved for userId: $userId, token: $token")
    }


    // Ambil token session
    val sessionToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }

    // Ambil user ID
    val sessionUserId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_ID_KEY]
    }

    // Ambil waktu kadaluarsa session
    val sessionExpiresAt: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[EXPIRES_AT_KEY]
    }

    // Hapus session saat logout
    suspend fun clearSession() {
        android.util.Log.e("SessionManager", "🗑 Clearing user session...")
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(EXPIRES_AT_KEY)

            // 🔹 Cetak Log saat Logout
            android.util.Log.d("SessionManager", "Token Cleared: User logged out")
        }
    }
}

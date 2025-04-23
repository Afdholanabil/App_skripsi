package com.example.app_skripsi.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.formDataStore: DataStore<Preferences> by preferencesDataStore(name = "form_session")

class FormSessionManager(private val context: Context) {

    // Keys untuk DataStore
    companion object {
        private val IS_SESSION_ACTIVE = booleanPreferencesKey("is_session_active")
        private val EMOTION = stringPreferencesKey("emotion")
        private val ACTIVITY = stringPreferencesKey("activity")
        private val CURRENT_STEP = stringPreferencesKey("current_step")

        // Keys untuk pertanyaan GAD-7
        private val GAD_PREFIX = "gad_answer_"
        private val GAD_TOTAL_SCORE = intPreferencesKey("gad_total_score")

        private val DETECTION_TYPE = stringPreferencesKey("detection_type")
    }

    // Status sesi aktif
    val isSessionActive: Flow<Boolean> = context.formDataStore.data.map { preferences ->
        preferences[IS_SESSION_ACTIVE] ?: false
    }

    // Emosi yang dipilih
    val emotion: Flow<String> = context.formDataStore.data.map { preferences ->
        preferences[EMOTION] ?: ""
    }

    // Aktivitas yang dipilih
    val activity: Flow<String> = context.formDataStore.data.map { preferences ->
        preferences[ACTIVITY] ?: ""
    }

    // Langkah saat ini
    val currentStep: Flow<String> = context.formDataStore.data.map { preferences ->
        preferences[CURRENT_STEP] ?: "permission" // Default ke step pertama
    }

    // Total skor GAD-7
    val gadTotalScore: Flow<Int> = context.formDataStore.data.map { preferences ->
        preferences[GAD_TOTAL_SCORE] ?: 0
    }

    // Flow untuk tipe deteksi
    val detectionType: Flow<String> = context.formDataStore.data
        .map { preferences -> preferences[DETECTION_TYPE] ?: "QUICK" }

    // Memulai sesi baru
    suspend fun startSession() {
        context.formDataStore.edit { preferences ->
            preferences[IS_SESSION_ACTIVE] = true
            preferences[CURRENT_STEP] = "permission"
        }
    }

    // Menyimpan pilihan emosi
    suspend fun saveEmotion(selectedEmotion: String) {
        context.formDataStore.edit { preferences ->
            preferences[EMOTION] = selectedEmotion
            preferences[CURRENT_STEP] = "emotion"
        }
    }

    // Menyimpan pilihan aktivitas
    suspend fun saveActivity(selectedActivity: String) {
        context.formDataStore.edit { preferences ->
            preferences[ACTIVITY] = selectedActivity
            preferences[CURRENT_STEP] = "activity"
        }
    }
    // Method to get emotion as a suspend function
    suspend fun getEmotion(): String {
        return emotion.first()
    }

    // Method to get activity as a suspend function
    suspend fun getActivity(): String {
        return activity.first()
    }

    // Menyimpan jawaban GAD
    suspend fun saveGadAnswer(questionNumber: Int, answerValue: Int) {
        if (questionNumber in 0..6) {
            val key = stringPreferencesKey("$GAD_PREFIX$questionNumber") // Ubah ke stringPreferencesKey
            context.formDataStore.edit { preferences ->
                preferences[key] = answerValue.toString() // Simpan sebagai string
                preferences[CURRENT_STEP] = "gad_$questionNumber"
            }
        }
    }

    // Mendapatkan jawaban GAD
    suspend fun getGadAnswer(questionNumber: Int): Int {
        if (questionNumber in 0..6) {
            val key = stringPreferencesKey("$GAD_PREFIX$questionNumber")
            val preferences = context.formDataStore.data.first()
            val strValue = preferences[key]

            Log.d("FormSessionManager", "Mengambil jawaban untuk pertanyaan $questionNumber. Raw value: $strValue")

            return try {
                val value = strValue?.toInt() ?: -1
                Log.d("FormSessionManager", "Jawaban pertanyaan $questionNumber = $value")
                value
            } catch (e: NumberFormatException) {
                Log.e("FormSessionManager", "Error parsing jawaban GAD $questionNumber: $strValue", e)
                -1
            }
        }
        return -1
    }

    // Menyimpan total skor GAD
    suspend fun saveGadTotalScore(totalScore: Int) {
        context.formDataStore.edit { preferences ->
            preferences[GAD_TOTAL_SCORE] = totalScore
            preferences[CURRENT_STEP] = "gad_completed"
        }
    }

    // Reset sesi
    suspend fun resetSession() {
        context.formDataStore.edit { preferences ->
            // Hapus seluruh data untuk memastikan benar-benar bersih
            preferences.clear()

            // Hapus secara spesifik kunci-kunci penting
            preferences[IS_SESSION_ACTIVE] = false
            preferences[CURRENT_STEP] = "permission"
            preferences[EMOTION] = ""
            preferences[ACTIVITY] = ""
            preferences[GAD_TOTAL_SCORE] = 0
            preferences[DETECTION_TYPE] = "QUICK" // Reset ke tipe default

            // Hapus semua jawaban GAD dengan eksplisit
            for (i in 0..6) {
                val key = stringPreferencesKey("$GAD_PREFIX$i")
                preferences.remove(key)
            }
        }

        // Cek apakah reset berhasil
        val testPref = context.formDataStore.data.first()
        Log.d("FormSessionManager", "Reset session - active: ${testPref[IS_SESSION_ACTIVE]}, step: ${testPref[CURRENT_STEP]}")
    }

    // Tambahkan di FormSessionManager
    suspend fun validateAllGadAnswers(): Boolean {
        for (i in 0..6) {
            val answer = getGadAnswer(i)
            if (answer < 0) {
                return false
            }
        }
        return true
    }

    // Simpan tipe deteksi
    suspend fun saveDetectionType(type: String) {
        context.formDataStore.edit { preferences ->
            preferences[DETECTION_TYPE] = type
        }
    }

    // Dapatkan tipe deteksi
    suspend fun getDetectionType(): String {
        return context.formDataStore.data.first()[DETECTION_TYPE] ?: "QUICK"
    }
}
package com.example.app_skripsi.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


// Extension function untuk DataStore
private val Context.routineSessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "routine_session_preferences"
)

class RoutineSessionManager(private val context: Context) {
    // Keys untuk DataStore
    companion object {
        private val SESSION_ACTIVE = booleanPreferencesKey("session_active")
        private val SESSION_TYPE = stringPreferencesKey("session_type") // "1_WEEK", "2_WEEKS", "1_MONTH"
        private val SESSION_START_DATE = longPreferencesKey("session_start_date")
        private val SESSION_END_DATE = longPreferencesKey("session_end_date")
        private val LAST_FORM_COMPLETION_DATE = stringPreferencesKey("last_form_completion_date")
    }

    // Flow untuk memantau status sesi
    val isSessionActive: Flow<Boolean> = context.routineSessionDataStore.data
        .map { preferences -> preferences[SESSION_ACTIVE] ?: false }

    // Flow untuk memantau tipe sesi
    val sessionType: Flow<String> = context.routineSessionDataStore.data
        .map { preferences -> preferences[SESSION_TYPE] ?: "" }

    // Flow untuk tanggal mulai sesi
    val sessionStartDate: Flow<Long> = context.routineSessionDataStore.data
        .map { preferences -> preferences[SESSION_START_DATE] ?: 0L }

    // Flow untuk tanggal akhir sesi
    val sessionEndDate: Flow<Long> = context.routineSessionDataStore.data
        .map { preferences -> preferences[SESSION_END_DATE] ?: 0L }

    // Flow untuk tanggal terakhir pengisian form
    val lastFormCompletionDate: Flow<String> = context.routineSessionDataStore.data
        .map { preferences -> preferences[LAST_FORM_COMPLETION_DATE] ?: "" }

    // Memulai sesi baru
    suspend fun startNewSession(sessionType: String) {
        val calendar = Calendar.getInstance()
        val startDate = calendar.timeInMillis

        // Menghitung tanggal akhir berdasarkan tipe sesi
        val endDate = when(sessionType) {
            "1_WEEK" -> {
                calendar.add(Calendar.DAY_OF_YEAR, 7)
                calendar.timeInMillis
            }
            "2_WEEKS" -> {
                calendar.add(Calendar.DAY_OF_YEAR, 14)
                calendar.timeInMillis
            }
            "1_MONTH" -> {
                calendar.add(Calendar.MONTH, 1)
                calendar.timeInMillis
            }
            else -> {
                calendar.add(Calendar.DAY_OF_YEAR, 7) // Default 1 minggu
                calendar.timeInMillis
            }
        }

        context.routineSessionDataStore.edit { preferences ->
            preferences[SESSION_ACTIVE] = true
            preferences[SESSION_TYPE] = sessionType
            preferences[SESSION_START_DATE] = startDate
            preferences[SESSION_END_DATE] = endDate
            preferences[LAST_FORM_COMPLETION_DATE] = "" // Reset tanggal terakhir pengisian
        }
    }

    suspend fun saveFormCompletionForToday() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        Log.d("RoutineSessionManager", "Saving form completion for date: $today")

        try {
            context.routineSessionDataStore.edit { preferences ->
                preferences[LAST_FORM_COMPLETION_DATE] = today
            }

            // Verifikasi penyimpanan
            val savedDate = context.routineSessionDataStore.data.first()[LAST_FORM_COMPLETION_DATE] ?: ""
            Log.d("RoutineSessionManager", "Verification - Saved completion date: $savedDate")

            if (savedDate != today) {
                Log.e("RoutineSessionManager", "ERROR: Failed to save completion date! Expected: $today, Actual: $savedDate")
            }
        } catch (e: Exception) {
            Log.e("RoutineSessionManager", "Error saving form completion date", e)
        }
    }

    suspend fun hasCompletedFormToday(): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val preferences = context.routineSessionDataStore.data.first()
        val lastDate = preferences[LAST_FORM_COMPLETION_DATE] ?: ""

        val result = lastDate == today
        Log.d("RoutineSessionManager", "Checking form completion - Today: $today, Last completion: $lastDate, Result: $result")

        return result
    }



    // Mengakhiri sesi
    suspend fun endSession() {
        context.routineSessionDataStore.edit { preferences ->
            preferences[SESSION_ACTIVE] = false
        }
    }


    // Cek apakah sesi masih aktif
    suspend fun isSessionStillActive(): Boolean {
        val preferences = context.routineSessionDataStore.data.first()
        val isActive = preferences[SESSION_ACTIVE] ?: false
        val endDate = preferences[SESSION_END_DATE] ?: 0L

        if (!isActive) return false

        val currentTime = System.currentTimeMillis()

        // Jika tanggal saat ini melebihi tanggal akhir, sesi berakhir
        if (currentTime > endDate) {
            endSession()
            return false
        }
        return true
    }

    // Mendapatkan hari ke berapa dari sesi
    suspend fun getCurrentSessionDay(): Int {
        val preferences = context.routineSessionDataStore.data.first()
        val startDate = preferences[SESSION_START_DATE] ?: 0L

        if (startDate == 0L) return 0

        val currentTime = System.currentTimeMillis()

        val startCalendar = Calendar.getInstance()
        startCalendar.timeInMillis = startDate

        val currentCalendar = Calendar.getInstance()
        currentCalendar.timeInMillis = currentTime

        // Reset time component untuk akurasi perhitungan hari
        startCalendar.set(Calendar.HOUR_OF_DAY, 0)
        startCalendar.set(Calendar.MINUTE, 0)
        startCalendar.set(Calendar.SECOND, 0)
        startCalendar.set(Calendar.MILLISECOND, 0)

        currentCalendar.set(Calendar.HOUR_OF_DAY, 0)
        currentCalendar.set(Calendar.MINUTE, 0)
        currentCalendar.set(Calendar.SECOND, 0)
        currentCalendar.set(Calendar.MILLISECOND, 0)

        val diffInMillis = currentCalendar.timeInMillis - startCalendar.timeInMillis
        return (diffInMillis / (24 * 60 * 60 * 1000)).toInt() + 1
    }

    // Mendapatkan tipe sesi dalam format yang lebih ramah pengguna
    suspend fun getSessionTypeDisplay(): String {
        val preferences = context.routineSessionDataStore.data.first()
        val typeValue = preferences[SESSION_TYPE] ?: ""

        return when(typeValue) {
            "1_WEEK" -> "1 Minggu"
            "2_WEEKS" -> "2 Minggu"
            "1_MONTH" -> "1 Bulan"
            else -> "Tidak diketahui"
        }
    }

    // Mendapatkan durasi sesi dalam hari
    suspend fun getSessionDurationInDays(): Int {
        val preferences = context.routineSessionDataStore.data.first()
        val typeValue = preferences[SESSION_TYPE] ?: ""

        return when(typeValue) {
            "1_WEEK" -> 7
            "2_WEEKS" -> 14
            "1_MONTH" -> 30
            else -> 7 // default 1 minggu
        }
    }
}
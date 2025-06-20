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
import com.example.app_skripsi.data.firebase.FirebaseService
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
        private val USER_ID = stringPreferencesKey("user_id")
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

    // Di kelas RoutineSessionManager, tambahkan fungsi berikut:

    // Method untuk mengakhiri sesi deteksi rutin secara menyeluruh (lokal dan Firebase)
    suspend fun endRoutineSession(firebaseService: FirebaseService): Boolean {
        try {
            // Dapatkan user ID yang tersimpan
            val userId = getUserId() ?: firebaseService.getCurrentUserId()
            if (userId == null) {
                Log.e("RoutineSessionManager", "User ID tidak tersedia")
                return false
            }

            Log.d("RoutineSessionManager", "Mengakhiri sesi untuk user: $userId")

            // Cari deteksi rutin yang aktif di Firebase
            val result = firebaseService.getActiveRoutineDetection(userId)

            if (result.isSuccess) {
                val activeRoutine = result.getOrNull()

                if (activeRoutine != null) {
                    // Update status di Firebase
                    Log.d("RoutineSessionManager", "Menonaktifkan dokumen rutin: ${activeRoutine.first}")
                    val updateResult = firebaseService.updateRoutineDetectionStatus(
                        userId,
                        activeRoutine.first,
                        false
                    )

                    if (updateResult.isSuccess) {
                        // Jika berhasil di Firebase, update juga DataStore
                        endSession()
                        Log.d("RoutineSessionManager", "Sesi rutin berhasil diakhiri")
                        return true
                    } else {
                        Log.e("RoutineSessionManager", "Gagal mengupdate status rutin di Firebase: ${updateResult.exceptionOrNull()?.message}")
                    }
                } else {
                    Log.d("RoutineSessionManager", "Tidak ada sesi rutin aktif di Firebase, hanya mengakhiri sesi lokal")
                    // Tidak ada deteksi rutin aktif di Firebase, tetap akhiri sesi lokal
                    endSession()
                    return true
                }
            } else {
                Log.e("RoutineSessionManager", "Error mengambil sesi rutin aktif: ${result.exceptionOrNull()?.message}")
            }

            return false
        } catch (e: Exception) {
            Log.e("RoutineSessionManager", "Error saat mengakhiri sesi rutin: ${e.message}")
            return false
        }
    }

    // Method untuk menyimpan user ID
    suspend fun setUserId(userId: String) {
        context.routineSessionDataStore.edit { preferences ->
            preferences[USER_ID] = userId
        }
        Log.d("RoutineSessionManager", "User ID telah disimpan: $userId")
    }

    // Method untuk mendapatkan user ID tersimpan
    suspend fun getUserId(): String? {
        return context.routineSessionDataStore.data.first()[USER_ID]
    }

    // Modifikasi startNewSession untuk menerima parameter userId
    suspend fun startNewSession(sessionType: String, userId: String) {
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
            preferences[USER_ID] = userId // Simpan user ID
        }

        Log.d("RoutineSessionManager", "Sesi baru dimulai untuk user: $userId, tipe: $sessionType")
    }



    // Method untuk validasi user
    suspend fun validateUser(currentUserId: String): Boolean {
        val storedUserId = getUserId()
        return storedUserId == currentUserId
    }

    // Method untuk sinkronisasi status dengan Firebase
    suspend fun syncWithFirebase(firebaseService: FirebaseService) {
        val userId = getUserId() ?: return

        try {
            // Cek apakah data lokal menunjukkan sesi aktif
            val isLocalActive = isSessionActive.first()

            if (isLocalActive) {
                // Cek status di Firebase
                val result = firebaseService.getActiveRoutineDetection(userId)
                if (result.isSuccess) {
                    val hasActiveRoutine = result.getOrNull() != null

                    // Jika di local active tapi di Firebase tidak ada yang active,
                    // update local menjadi tidak aktif
                    if (!hasActiveRoutine) {
                        endSession()
                        Log.d("RoutineSessionManager", "No active routine found in Firebase, ending local session")
                    }
                }
            } else {
                // Local tidak aktif, pastikan tidak ada yang aktif di Firebase
                val result = firebaseService.getActiveRoutineDetection(userId)
                if (result.isSuccess && result.getOrNull() != null) {
                    // Ada yang aktif di Firebase tapi tidak di local, update Firebase
                    val activeRoutine = result.getOrNull()
                    if (activeRoutine != null) {
                        firebaseService.updateRoutineDetectionStatus(userId, activeRoutine.first, false)
                        Log.d("RoutineSessionManager", "Found active routine in Firebase but local is inactive, deactivating in Firebase")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("RoutineSessionManager", "Error syncing with Firebase: ${e.message}")
        }
    }

    // Method untuk me-reset semua data saat logout
    suspend fun clearAllData() {
        context.routineSessionDataStore.edit { preferences ->
            preferences.clear()
        }
        Log.d("RoutineSessionManager", "All routine session data cleared")
    }



    // Method untuk pengecekan apakah hari ini adalah hari terakhir sesi
    suspend fun isTodayLastDayOfSession(): Boolean {
        val isActive = isSessionStillActive()
        if (!isActive) return false

        val preferences = context.routineSessionDataStore.data.first()
        val endDate = preferences[SESSION_END_DATE] ?: 0L

        // Buat kalender untuk tanggal akhir dan hari ini
        val endCalendar = Calendar.getInstance()
        endCalendar.timeInMillis = endDate
        endCalendar.set(Calendar.HOUR_OF_DAY, 0)
        endCalendar.set(Calendar.MINUTE, 0)
        endCalendar.set(Calendar.SECOND, 0)
        endCalendar.set(Calendar.MILLISECOND, 0)

        val todayCalendar = Calendar.getInstance()
        todayCalendar.set(Calendar.HOUR_OF_DAY, 0)
        todayCalendar.set(Calendar.MINUTE, 0)
        todayCalendar.set(Calendar.SECOND, 0)
        todayCalendar.set(Calendar.MILLISECOND, 0)

        // Cek apakah tanggal sama
        return endCalendar.timeInMillis == todayCalendar.timeInMillis
    }

    // Fungsi untuk mengakhiri sesi di DataStore dan Firebase
    suspend fun endRoutineSessionCompletely(firebaseService: FirebaseService): Boolean {
        try {
            val userId = getUserId() ?: return false

            // Dapatkan dokumen rutin yang aktif
            val result = firebaseService.getActiveRoutineDetection(userId)
            if (result.isSuccess) {
                val activeDetection = result.getOrNull()
                if (activeDetection != null) {
                    // Update status di Firebase
                    val updateResult = firebaseService.updateRoutineDetectionStatus(
                        userId,
                        activeDetection.first,
                        false
                    )

                    if (updateResult.isSuccess) {
                        // Update local storage
                        endSession()
                        Log.d("RoutineSessionManager", "Session ended successfully")
                        return true
                    }
                }
            }

            return false
        } catch (e: Exception) {
            Log.e("RoutineSessionManager", "Error ending routine session", e)
            return false
        }
    }

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
            try {
                // Akhiri sesi di DataStore
                endSession()

                // Dapatkan userId
                val userId = getUserId()

                // Jika userId tersedia, update juga di Firebase
                if (userId != null) {
                    // Ini perlu ditambahkan di method ini atau di class yang menggunakannya
                    val firebaseService = FirebaseService()
                    val result = firebaseService.getActiveRoutineDetection(userId)

                    if (result.isSuccess && result.getOrNull() != null) {
                        val activeRoutine = result.getOrNull()!!
                        firebaseService.updateRoutineDetectionStatus(userId, activeRoutine.first, false)
                        Log.d("RoutineSessionManager", "Routine session ended in Firestore due to end date")
                    }
                }
            } catch (e: Exception) {
                Log.e("RoutineSessionManager", "Error ending session in Firestore: ${e.message}")
            }

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

    suspend fun forceEndCurrentSession(): Boolean {
        try {
            // Update local DataStore
            context.routineSessionDataStore.edit { preferences ->
                preferences[SESSION_ACTIVE] = false
            }

            Log.d("RoutineSessionManager", "Session successfully ended in DataStore")
            return true
        } catch (e: Exception) {
            Log.e("RoutineSessionManager", "Error ending session", e)
            return false
        }
    }

    // Get document ID for current routine session
    suspend fun getCurrentRoutineDocumentId(): String? {
        val isActive = isSessionStillActive()
        if (!isActive) return null

        // Menggunakan startDate sebagai identifier
        val startDate = sessionStartDate.first()
        return "deteksi_rutin_${startDate}"
    }


    // Tambahkan fungsi untuk cek apakah semua hari dalam periode sudah diisi
    suspend fun getCompletedSessionProgress(firebaseService: FirebaseService): Pair<Int, Int> {
        val userId = firebaseService.getCurrentUserId() ?: return Pair(0, 0)

        try {
            // Dapatkan dokumen deteksi rutin yang aktif
            val result = firebaseService.getActiveRoutineDetection(userId)
            if (result.isSuccess) {
                val activeDetection = result.getOrNull()
                if (activeDetection != null) {
                    val totalDays = getSessionDurationInDays()
                    val completedDays = activeDetection.second.deteksiHarian.size
                    return Pair(completedDays, totalDays)
                }
            }

            return Pair(0, 0)
        } catch (e: Exception) {
            Log.e("RoutineSessionManager", "Error getting session progress", e)
            return Pair(0, 0)
        }
    }

    // Tambahkan di RoutineSessionManager.kt
    suspend fun restoreSessionFromFirebase(firebaseService: FirebaseService): Boolean {
        try {
            // Ambil user ID saat ini
            val userId = firebaseService.getCurrentUserId() ?: return false

            // Simpan user ID ke DataStore
            setUserId(userId)

            // Cek apakah ada sesi aktif di Firebase
            val result = firebaseService.getActiveRoutineDetection(userId)

            if (result.isSuccess && result.getOrNull() != null) {
                val activeRoutine = result.getOrNull()!!

                // Ada sesi aktif, restore data ke DataStore
                val sessionType = activeRoutine.second.periode
                val startDate = activeRoutine.second.tanggalMulai.toDate().time
                val endDate = activeRoutine.second.tanggalSelesai.toDate().time

                // Hitung progress sesi (untuk completion date)
                val dayEntries = activeRoutine.second.deteksiHarian

                // Ambil tanggal sesi terbaru jika ada
                val latestDateEntry = dayEntries.entries
                    .maxByOrNull { it.key.toIntOrNull() ?: 0 }

                val lastCompletionDate = if (latestDateEntry != null) {
                    val latestDetection = latestDateEntry.value
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(latestDetection.tanggal.toDate())
                } else {
                    ""
                }

                // Restore data ke DataStore
                context.routineSessionDataStore.edit { preferences ->
                    preferences[SESSION_ACTIVE] = true
                    preferences[SESSION_TYPE] = sessionType
                    preferences[SESSION_START_DATE] = startDate
                    preferences[SESSION_END_DATE] = endDate
                    preferences[LAST_FORM_COMPLETION_DATE] = lastCompletionDate
                    preferences[USER_ID] = userId
                }

                Log.d("RoutineSessionManager", "Berhasil restore sesi dari Firebase: tipe=$sessionType, " +
                        "hari ke-${dayEntries.size}, completion date=$lastCompletionDate")

                return true
            } else {
                Log.d("RoutineSessionManager", "Tidak ada sesi aktif di Firebase untuk dipulihkan")
                return false
            }
        } catch (e: Exception) {
            Log.e("RoutineSessionManager", "Error merestorasi sesi: ${e.message}")
            return false
        }
    }

}
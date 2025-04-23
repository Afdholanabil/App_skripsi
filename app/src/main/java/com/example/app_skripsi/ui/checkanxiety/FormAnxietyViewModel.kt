package com.example.app_skripsi.ui.checkanxiety

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_skripsi.data.local.FormSessionManager
import com.example.app_skripsi.data.local.RoutineSessionManager
import com.example.app_skripsi.data.model.RoutineDetectionModel
import com.example.app_skripsi.data.repository.AnxietyRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FormAnxietyViewModel(
    private val anxietyRepository: AnxietyRepository,
    private val formSessionManager: FormSessionManager,
    private val routineSessionManager: RoutineSessionManager
) : ViewModel() {

    // LiveData untuk UI state
    private val _selectedEmotion = MutableLiveData<String>()
    val selectedEmotion: LiveData<String> get() = _selectedEmotion

    private val _selectedActivity = MutableLiveData<String>()
    val selectedActivity: LiveData<String> get() = _selectedActivity

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private val _navigateToResult = MutableLiveData<Boolean>()
    val navigateToResult: LiveData<Boolean> get() = _navigateToResult

    private val _activeRoutineDetection = MutableLiveData<Pair<String, RoutineDetectionModel>?>()
    val activeRoutineDetection: LiveData<Pair<String, RoutineDetectionModel>?> get() = _activeRoutineDetection

    private val _routineSessionInfo = MutableLiveData<Triple<String, Int, Int>>() // type, currentDay, totalDays
    val routineSessionInfo: LiveData<Triple<String, Int, Int>> get() = _routineSessionInfo

    // LiveData untuk menyimpan jawaban GAD untuk diteruskan ke activity hasil
    private val _gadAnswersList = MutableLiveData<ArrayList<Int>>()
    val gadAnswersList: LiveData<ArrayList<Int>> get() = _gadAnswersList

    // Fungsi untuk mengatur emosi yang dipilih
    fun setSelectedEmotion(emotion: String) {
        _selectedEmotion.value = emotion
        viewModelScope.launch {
            formSessionManager.saveEmotion(emotion)
        }
    }

    // Fungsi untuk mengatur aktivitas yang dipilih
    fun setSelectedActivity(activity: String) {
        _selectedActivity.value = activity
        viewModelScope.launch {
            formSessionManager.saveActivity(activity)
        }
    }

    // Memeriksa apakah ada sesi rutin yang aktif
    fun checkActiveRoutineSession() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val result = anxietyRepository.getActiveRoutineDetection()
                if (result.isSuccess) {
                    _activeRoutineDetection.value = result.getOrNull()

                    // Update info sesi rutin jika ada
                    updateRoutineSessionInfo()
                }
            } catch (e: Exception) {
                Log.e("FormAnxietyViewModel", "Error checking active routine: ${e.message}")
                _errorMessage.value = "Gagal memeriksa status sesi rutin: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Membuat sesi deteksi rutin baru
    fun createRoutineSession(sessionType: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Log untuk debugging
                Log.d("FormAnxietyViewModel", "Creating routine session with type: $sessionType")

                // Inisiasi sesi lokal saja
                routineSessionManager.startNewSession(sessionType)
                Log.d("FormAnxietyViewModel", "Local routine session created successfully")

                // Dokumen Firestore akan dibuat nanti setelah form selesai
                checkActiveRoutineSession()
            } catch (e: Exception) {
                Log.e("FormAnxietyViewModel", "Error creating routine session: ${e.message}", e)
                _errorMessage.value = "Gagal membuat sesi rutin: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Update informasi sesi rutin
    private fun updateRoutineSessionInfo() {
        viewModelScope.launch {
            val activeRoutine = _activeRoutineDetection.value ?: return@launch

            try {
                val currentDayResult = anxietyRepository.getCurrentDayInRoutineSession(activeRoutine.first)
                val durationResult = anxietyRepository.getRoutineSessionDuration(activeRoutine.first)

                if (currentDayResult.isSuccess && durationResult.isSuccess) {
                    val currentDay = currentDayResult.getOrNull() ?: 1
                    val totalDays = durationResult.getOrNull() ?: 7
                    val sessionType = activeRoutine.second.periode

                    _routineSessionInfo.value = Triple(sessionType, currentDay, totalDays)
                }
            } catch (e: Exception) {
                Log.e("FormAnxietyViewModel", "Error updating session info: ${e.message}")
            }
        }
    }

    // Memeriksa apakah deteksi rutin sudah dilakukan hari ini
    fun checkIfRoutineDetectionCompletedToday() {
        viewModelScope.launch {
            val activeRoutine = _activeRoutineDetection.value ?: return@launch

            try {
                val checkResult = anxietyRepository.hasCompletedRoutineDetectionToday(activeRoutine.first)
                if (checkResult.isSuccess) {
                    val hasCompleted = checkResult.getOrNull() ?: false
                    if (hasCompleted) {
                        routineSessionManager.saveFormCompletionForToday()
                    }
                }
            } catch (e: Exception) {
                Log.e("FormAnxietyViewModel", "Error checking detection completion: ${e.message}")
            }
        }
    }

    fun saveAnxietyDetection(gadAnswers: List<Int>, totalScore: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val emotion = formSessionManager.emotion.first()
                val activity = formSessionManager.activity.first()
                val detectionType = formSessionManager.getDetectionType()

                // Log untuk debugging
                Log.d("FormAnxietyViewModel", "Saving ${detectionType} detection - Emotion: $emotion, Activity: $activity, Score: $totalScore")
                Log.d("FormAnxietyViewModel", "GAD Answers: $gadAnswers")

                // Simpan jawaban dan total skor ke DataStore untuk backup
                gadAnswers.forEachIndexed { index, answer ->
                    formSessionManager.saveGadAnswer(index, answer)
                }
                formSessionManager.saveGadTotalScore(totalScore)

                // Tidak perlu menyimpan data di sini, cukup tandai selesai untuk hari ini jika rutin
                if (detectionType == "ROUTINE") {
                    routineSessionManager.saveFormCompletionForToday()
                } else {
                    // Untuk deteksi singkat, tetap simpan data
                    anxietyRepository.addShortDetection(emotion, activity, gadAnswers, totalScore)
                }

                // Navigasi ke hasil
                _navigateToResult.value = true
            } catch (e: Exception) {
                Log.e("FormAnxietyViewModel", "Error saving detection: ${e.message}")
                _errorMessage.value = "Gagal menyimpan deteksi: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Mendapatkan deteksi rutin aktif
    fun getActiveRoutineSessionInfo(): Pair<String, Triple<Int, Int, String>>? {
        val activeRoutine = _activeRoutineDetection.value ?: return null
        val sessionInfo = _routineSessionInfo.value ?: return null

        val sessionType = when(sessionInfo.first) {
            "1_WEEK" -> "1 Minggu"
            "2_WEEKS" -> "2 Minggu"
            "1_MONTH" -> "1 Bulan"
            else -> sessionInfo.first
        }

        return Pair(activeRoutine.first, Triple(sessionInfo.second, sessionInfo.third, sessionType))
    }

    // Validasi apakah semua jawaban GAD sudah diisi
    fun validateGadAnswers(gadAnswers: List<Int?>): Boolean {
        // Pastikan semua pertanyaan GAD telah dijawab
        return gadAnswers.size == 7 && gadAnswers.all { it != null }
    }

    // Reset navigasi ke hasil
    fun resetNavigationState() {
        _navigateToResult.value = false
    }

    // Mengecek apakah user bisa melakukan deteksi rutin hari ini
    fun canDoRoutineDetectionToday(): Boolean {
        var canDo = false
        viewModelScope.launch {
            // Cek apakah ada sesi rutin aktif
            val isSessionActive = routineSessionManager.isSessionStillActive()
            if (!isSessionActive) return@launch

            // Cek apakah sudah mengisi deteksi hari ini
            val hasCompletedToday = routineSessionManager.hasCompletedFormToday()
            canDo = !hasCompletedToday
        }
        return canDo
    }

    // Load data dari DataStore
    fun loadSavedData() {
        viewModelScope.launch {
            val emotion = formSessionManager.emotion.first()
            if (emotion.isNotEmpty()) {
                _selectedEmotion.value = emotion
            }

            val activity = formSessionManager.activity.first()
            if (activity.isNotEmpty()) {
                _selectedActivity.value = activity
            }
        }
    }

    // Untuk debugging
    fun getDetectionTypeInfo() {
        viewModelScope.launch {
            val detectionType = formSessionManager.getDetectionType()
            Log.d("FormAnxietyViewModel", "Current detection type: $detectionType")

            val isRoutineActive = routineSessionManager.isSessionStillActive()
            Log.d("FormAnxietyViewModel", "Is routine session active: $isRoutineActive")

            if (isRoutineActive) {
                val sessionType = routineSessionManager.getSessionTypeDisplay()
                val currentDay = routineSessionManager.getCurrentSessionDay()
                val totalDays = routineSessionManager.getSessionDurationInDays()
                val lastCompletionDate = routineSessionManager.lastFormCompletionDate.first()

                Log.d("FormAnxietyViewModel", "Session type: $sessionType")
                Log.d("FormAnxietyViewModel", "Current day: $currentDay of $totalDays")
                Log.d("FormAnxietyViewModel", "Last completion date: $lastCompletionDate")
            }
        }
    }

    // Di FormAnxietyViewModel, pada saat menangani navigasi ke hasil
    fun navigateToResultScreen() {
        viewModelScope.launch {
            val emotion = formSessionManager.emotion.first()
            val activity = formSessionManager.activity.first()
            val totalScore = formSessionManager.gadTotalScore.first()

            // Dapatkan jawaban GAD
            val gadAnswers = mutableListOf<Int>()
            for (i in 0..6) {
                val answer = formSessionManager.getGadAnswer(i)
                if (answer > 0) {
                    // Konversi dari skala 1-4 ke skala 0-3
                    gadAnswers.add(answer - 1)
                } else {
                    gadAnswers.add(0) // Default 0 jika tidak ada jawaban
                }
            }

            // Sediakan data untuk HasilAnxietyShortActivity
//            _resultData.value = HasilAnxietyData(
//                totalScore = totalScore,
//                emotion = emotion,
//                activity = activity,
//                gadAnswers = gadAnswers
//            )

            _navigateToResult.value = true
        }
    }

    // Data class untuk menampung data hasil
    data class HasilAnxietyData(
        val totalScore: Int,
        val emotion: String,
        val activity: String,
        val gadAnswers: List<Int>
    )
}
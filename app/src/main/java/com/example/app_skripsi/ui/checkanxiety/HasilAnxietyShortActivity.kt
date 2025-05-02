package com.example.app_skripsi.ui.checkanxiety

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.app_skripsi.R
import com.example.app_skripsi.data.firebase.FirebaseService
import com.example.app_skripsi.data.local.FormSessionManager
import com.example.app_skripsi.data.local.RoutineSessionManager
import com.example.app_skripsi.data.model.DailyDetectionData
import com.example.app_skripsi.data.model.RoutineDetectionModel
import com.example.app_skripsi.data.repository.AnxietyRepository
import com.example.app_skripsi.databinding.ActivityHasilAnxietyShortBinding
import com.example.app_skripsi.utils.NotificationSchedulerManager
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class HasilAnxietyShortActivity : AppCompatActivity() {
    private var _binding: ActivityHasilAnxietyShortBinding? = null
    private val binding get() = _binding!!

    private lateinit var formSessionManager: FormSessionManager
    private lateinit var routineSessionManager: RoutineSessionManager

    // Flag untuk mencegah pemrosesan ganda
    companion object {
        private val isProcessing = AtomicBoolean(false)
        private const val TAG = "HasilAnxietyActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityHasilAnxietyShortBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left + v.paddingLeft, systemBars.top + v.paddingTop,
                systemBars.right + v.paddingRight, systemBars.bottom + v.paddingBottom)
            insets
        }

        // Inisialisasi manager
        formSessionManager = FormSessionManager(this)
        routineSessionManager = RoutineSessionManager(this)

        // Cek apakah proses sudah berjalan untuk mencegah duplikasi
        if (isProcessing.compareAndSet(false, true)) {
            try {
                processAnxietyResults()
            } finally {
                // Pastikan flag selalu direset
                isProcessing.set(false)
            }
        } else {
            Log.d(TAG, "Proses sudah berjalan, mencegah pemrosesan ganda")
        }

        // Back button
        binding.btnBack.setOnClickListener {
            // Reset session sebelum kembali
            lifecycleScope.launch {
                formSessionManager.resetSession()
                finish()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d(TAG, "onNewIntent dipanggil, mencegah pemrosesan ulang")
        // Tidak perlu memproses ulang, cukup tampilkan UI
    }

    private fun processAnxietyResults() {
        // Ambil data dari intent
        val totalScore = intent.getIntExtra("TOTAL_SCORE", 0)
        val emotion = intent.getStringExtra("EMOTION") ?: "Tidak Diketahui"
        val activity = intent.getStringExtra("ACTIVITY") ?: "Tidak Diketahui"
        val gadAnswers = intent.getIntegerArrayListExtra("GAD_ANSWERS") ?: arrayListOf()

        Log.d(TAG, "Data diterima - Score: $totalScore, Emotion: $emotion, Activity: $activity, GAD size: ${gadAnswers.size}")

        // Validasi data dan gunakan data dari session jika perlu
        lifecycleScope.launch {
            var finalScore = totalScore
            var finalEmotion = emotion
            var finalActivity = activity
            var finalGadAnswers = gadAnswers

            // Jika data intent tidak valid, gunakan data dari FormSessionManager
            if (totalScore == 0 || emotion == "Tidak Diketahui" || activity == "Tidak Diketahui" || gadAnswers.isEmpty()) {
                Log.d(TAG, "Data intent tidak valid, menggunakan data dari FormSessionManager")

                // Menggunakan gadTotalScore.first() untuk mendapatkan nilai dari Flow
                finalScore = formSessionManager.gadTotalScore.first()
                finalEmotion = formSessionManager.emotion.first()
                finalActivity = formSessionManager.activity.first()

                // Ambil jawaban GAD-7 dari formSessionManager
                val retrievedAnswers = ArrayList<Int>()
                for (i in 0..6) {
                    val answer = formSessionManager.getGadAnswer(i)
                    // Konversi ke skala 0-3 jika nilai valid
                    if (answer > 0) {
                        retrievedAnswers.add(answer - 1)
                    } else {
                        retrievedAnswers.add(0) // Default ke 0 jika tidak valid
                    }
                }
                finalGadAnswers = retrievedAnswers

                Log.d(TAG, "Data dari FormSessionManager - Score: $finalScore, Emotion: $finalEmotion, Activity: $finalActivity, GAD size: ${finalGadAnswers.size}")
            }

            // Set UI dengan data yang sudah divalidasi
            updateUI(finalScore, finalEmotion, finalActivity, finalGadAnswers)

            // Proses data untuk deteksi rutin jika perlu
            processRoutineDetectionIfNeeded(finalScore, finalEmotion, finalActivity, finalGadAnswers)
        }
    }

    private suspend fun processRoutineDetectionIfNeeded(
        totalScore: Int,
        emotion: String,
        activity: String,
        gadAnswers: List<Int>
    ) {
        try {
            val detectionType = formSessionManager.getDetectionType()

            if (detectionType == "ROUTINE") {
                val firebaseService = FirebaseService()
                val anxietyRepository = AnxietyRepository(firebaseService)

                // Cek apakah ini hari pertama dari sesi rutin
                val currentDay = routineSessionManager.getCurrentSessionDay()
                val isFirstDay = currentDay == 1

                Log.d(TAG, "Processing routine detection for day $currentDay")

                if (isFirstDay) {
                    // Ambil periode sesi dari DataStore
                    val sessionType = routineSessionManager.sessionType.first()
                    Log.d(TAG, "Creating routine detection with session type: $sessionType")

                    // Buat model data untuk hari ini
                    val calendar = Calendar.getInstance()
                    val currentDate = Date()
                    val severity = getSeverityLevel(totalScore)
                    val dayOfWeek = getDayOfWeek(calendar)

                    // Buat DailyDetectionData
                    val dailyData = DailyDetectionData(
                        emosi = emotion,
                        kegiatan = activity,
                        gad1 = gadAnswers.getOrElse(0) { 0 },
                        gad2 = gadAnswers.getOrElse(1) { 0 },
                        gad3 = gadAnswers.getOrElse(2) { 0 },
                        gad4 = gadAnswers.getOrElse(3) { 0 },
                        gad5 = gadAnswers.getOrElse(4) { 0 },
                        gad6 = gadAnswers.getOrElse(5) { 0 },
                        gad7 = gadAnswers.getOrElse(6) { 0 },
                        tanggal = Timestamp(currentDate),
                        totalSkor = totalScore,
                        severity = severity
                    )

                    // Map untuk data harian dengan hari 1
                    val dailyDataMap = mapOf("1" to dailyData)

                    // Membuat model rutin dengan data hari pertama
                    val startDate = routineSessionManager.sessionStartDate.first()
                    val endDate = routineSessionManager.sessionEndDate.first()

                    val routineDetection = RoutineDetectionModel(
                        aktif = true,
                        deteksiHarian = dailyDataMap,
                        hariSkorRendah = dayOfWeek,
                        hariSkorTinggi = dayOfWeek,
                        skorRendah = totalScore,
                        skorTinggi = totalScore,
                        periode = sessionType,
                        tanggalMulai = Timestamp(Date(startDate)),
                        tanggalSelesai = Timestamp(Date(endDate))
                    )

                    // Tambahkan ke Firestore
                    val userId = firebaseService.getCurrentUserId()
                    if (userId != null) {
                        try {
                            val result = firebaseService.addRoutineDetection(userId, routineDetection)
                            if (result.isSuccess) {
                                Log.d(TAG, "Successfully created routine detection document")
                            } else {
                                Log.e(TAG, "Failed to create routine detection: ${result.exceptionOrNull()?.message}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error creating routine detection", e)
                        }
                    } else {
                        Log.e(TAG, "User not logged in, cannot create routine detection")
                    }
                } else {
                    // Update dokumen yang sudah ada untuk hari saat ini
                    val isSessionActive = routineSessionManager.isSessionStillActive()
                    if (isSessionActive) {
                        // Ambil dokumen aktif dari repository
                        val activeRoutineResult = anxietyRepository.getActiveRoutineDetection()
                        if (activeRoutineResult.isSuccess) {
                            val activeRoutine = activeRoutineResult.getOrNull()
                            if (activeRoutine != null) {
                                // Tambahkan data untuk hari ini ke dokumen yang sudah ada
                                val result = anxietyRepository.addDailyDataToExistingRoutine(
                                    activeRoutine.first,
                                    emotion,
                                    activity,
                                    gadAnswers,
                                    totalScore,
                                    routineSessionManager
                                )

                                if (result.isSuccess) {
                                    Log.d(TAG, "Successfully added day $currentDay data to existing routine")
                                    // Setelah berhasil menyimpan data hari ini // Cek apakah ini adalah hari terakhir dan semua data sudah lengkap
                                    val totalDays = routineSessionManager.getSessionDurationInDays()
                                    val currentDay = routineSessionManager.getCurrentSessionDay()

                                    if (currentDay >= totalDays) {
                                        // Ini hari terakhir, akhiri sesi
                                        val userId = firebaseService.getCurrentUserId()
                                        val activeRoutineResult = anxietyRepository.getActiveRoutineDetection()

                                        if (activeRoutineResult.isSuccess && activeRoutineResult.getOrNull() != null) {
                                            val activeRoutine = activeRoutineResult.getOrNull()!!

                                            // Cancel reminders setelah sesi selesai
                                            val notificationManager = NotificationSchedulerManager(this)
                                            notificationManager.cancelRoutineFormAlarms()

                                            Log.d(TAG, "Routine session completed and reminders cancelled")

                                            // Update status di Firestore
                                            firebaseService.updateRoutineDetectionStatus(userId!!, activeRoutine.first, false)

                                            // Update local storage
                                            routineSessionManager.endSession()

                                            Log.d(TAG, "Routine session completed and ended automatically")
                                        }
                                    }
                                } else {
                                    Log.e(TAG, "Failed to add day data: ${result.exceptionOrNull()?.message}")
                                }
                            }
                        }
                    }
                }

                // Tandai sebagai telah mengisi form hari ini
                routineSessionManager.saveFormCompletionForToday()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing routine detection", e)
        }
    }

    private fun updateUI(
        totalScore: Int,
        emotion: String,
        activity: String,
        gadAnswers: List<Int>
    ) {
        // Set current date
        val currentDate = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID")).format(Date())
        binding.tvDate.text = "Hari: $currentDate"

        // Set emotion and activity
        binding.tvEmotion.text = "Emosi: $emotion"
        binding.tvActivity.text = "Kegiatan: $activity"

        // Set total score and anxiety level
        binding.tvTotalSkor.text = "Total Skor GAD-7: $totalScore"
        setAnxietyLevelAndAdvice(totalScore)

        // Set individual GAD scores
        setGadScores(gadAnswers)
    }

    // Helper function
    private fun getSeverityLevel(totalScore: Int): String {
        return when {
            totalScore in 0..4 -> "Minimal"
            totalScore in 5..9 -> "Ringan"
            totalScore in 10..14 -> "Moderate"
            totalScore >= 15 -> "Parah"
            else -> "Tidak diketahui"
        }
    }

    private fun getDayOfWeek(calendar: Calendar): String {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            Calendar.SUNDAY -> "Minggu"
            else -> ""
        }
    }

    private fun setAnxietyLevelAndAdvice(totalScore: Int) {
        val (level, color, advice) = when {
            totalScore in 0..4 -> Triple("Minimal", R.color.thirtary,
                "Tingkat kecemasan Anda minimal. Terus pertahankan kesehatan mental dan gaya hidup sehat.")
            totalScore in 5..9 -> Triple("Ringan", R.color.blueSecondary,
                "Anda mungkin bisa mengelola ini secara mandiri dengan teknik manajemen stres, latihan relaksasi, dan perubahan gaya hidup seperti tidur cukup dan makan makanan sehat.")
            totalScore in 10..14 -> Triple("Sedang", R.color.bluePrimary,
                "Tingkat kecemasan Anda moderat. Pertimbangkan untuk mencari dukungan dari teman, keluarga, atau profesional kesehatan mental.")
            else -> Triple("Berat", R.color.purplePrimary,
                "Tingkat kecemasan Anda tinggi. Sangat disarankan untuk segera berkonsultasi dengan profesional kesehatan mental.")
        }

        binding.btnAnxietyManagement.text = "Kecemasan $level"
        binding.btnAnxietyManagement.backgroundTintList =
            android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this,color))
        binding.tvAdvice.text = advice
    }

    private fun setGadScores(gadAnswers: List<Int>) {
        // Clear existing views
        binding.layoutGADScores.removeAllViews()

        // Jika gadAnswers kosong, tampilkan pesan bahwa data tidak tersedia
        if (gadAnswers.isEmpty()) {
            val textView = TextView(this).apply {
                text = "Data jawaban GAD tidak tersedia"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(ContextCompat.getColor(this@HasilAnxietyShortActivity, R.color.gray800))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
            }
            binding.layoutGADScores.addView(textView)
            return
        }

        // Pertanyaan GAD-7
        val gadQuestions = listOf(
            "Merasa gugup, cemas, atau tegang",
            "Tidak mampu menghentikan atau mengendalikan rasa khawatir",
            "Terlalu mengkhawatirkan berbagai hal",
            "Sulit untuk rileks",
            "Sangat gelisah sehingga sulit untuk duduk diam",
            "Menjadi mudah tersinggung atau mudah marah",
            "Merasa takut, seolah-olah ada sesuatu yang buruk mungkin terjadi"
        )

        // Add GAD scores dynamically
        gadQuestions.forEachIndexed { index, question ->
            // Pastikan index masih dalam range gadAnswers
            if (index < gadAnswers.size) {
                val textView = TextView(this).apply {
                    text = "GAD-${index + 1}: $question - ${getGadScoreDescription(gadAnswers[index])}"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    setTextColor(ContextCompat.getColor(this@HasilAnxietyShortActivity, R.color.gray800))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 8, 0, 8)
                    }
                }
                binding.layoutGADScores.addView(textView)
            }
        }
    }

    private fun getGadScoreDescription(score: Int): String {
        return when (score) {
            0 -> "Tidak Pernah"
            1 -> "Beberapa Hari"

            2 -> "Lebih dari Setengah Hari"
            3 -> "Hampir Setiap Hari"
            else -> "Tidak Diketahui"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
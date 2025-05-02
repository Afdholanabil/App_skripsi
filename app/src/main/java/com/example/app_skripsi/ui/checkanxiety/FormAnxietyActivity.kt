package com.example.app_skripsi.ui.checkanxiety

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.app_skripsi.R
import com.example.app_skripsi.databinding.ActivityFormAnxietyBinding
import androidx.viewpager2.widget.ViewPager2
import com.example.app_skripsi.data.firebase.FirebaseService
import com.example.app_skripsi.data.local.FormSessionManager
import com.example.app_skripsi.data.local.RoutineSessionManager
import com.example.app_skripsi.data.repository.AnxietyRepository
import com.example.app_skripsi.utils.NotificationSchedulerManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FormAnxietyActivity : AppCompatActivity() {
    private var _binding: ActivityFormAnxietyBinding? = null
    private val binding get() = _binding!!

    private lateinit var formSessionManager: FormSessionManager
    private lateinit var routineSessionManager: RoutineSessionManager
    var detectionType: String = "QUICK" // Default ke deteksi singkat

    lateinit var viewModel: FormAnxietyViewModel
    private lateinit var firebaseService: FirebaseService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityFormAnxietyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi formSessionManager dan routineSessionManager
        formSessionManager = FormSessionManager(this)
        routineSessionManager = RoutineSessionManager(this)
        firebaseService = FirebaseService()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bottomNav = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left + v.paddingLeft, systemBars.top + v.paddingTop,
                systemBars.right + v.paddingRight, bottomNav.bottom + v.paddingBottom)
            insets
        }

        // Hide status bar & navigation bar
        hideSystemUI()

        // Inisialisasi repository dan managers
        val firebaseService = FirebaseService()
        val anxietyRepository = AnxietyRepository(firebaseService)
        formSessionManager = FormSessionManager(this)
        routineSessionManager = RoutineSessionManager(this)

        // Inisialisasi ViewModel dengan factory
        val factory = FormAnxietyViewModelFactory(anxietyRepository, formSessionManager, routineSessionManager)
        viewModel = ViewModelProvider(this, factory)[FormAnxietyViewModel::class.java]


// Ambil tipe deteksi dari intent
        detectionType = intent.getStringExtra("DETECTION_TYPE") ?: "QUICK"
        Log.d(TAG, "Detection type from Intent: $detectionType")

        // Jika deteksi rutin, cek dulu apakah sudah mengisi hari ini
        // Di dalam onCreate di FormAnxietyActivity, bagian deteksi rutin
        // Di FormAnxietyActivity.onCreate
        if (detectionType == "ROUTINE") {
            lifecycleScope.launch {
                try {
                    val isSessionActive = routineSessionManager.isSessionStillActive()
                    Log.d(TAG, "Routine detection requested, session active: $isSessionActive")

                    if (!isSessionActive) {
                        // Jika belum ada sesi aktif, tampilkan dialog pemilihan
                        Log.d(TAG, "No active routine session, showing dialog")
                        showSessionSelectionDialog()
                        return@launch
                    }

                    // Ada sesi aktif, cek apakah sudah mengisi hari ini
                    val hasCompletedToday = routineSessionManager.hasCompletedFormToday()
                    Log.d(TAG, "Active session found, completed today: $hasCompletedToday")

                    if (hasCompletedToday) {
                        AlertDialog.Builder(this@FormAnxietyActivity)
                            .setTitle("Deteksi Sudah Dilakukan")
                            .setMessage("Anda sudah mengisi form deteksi kecemasan untuk hari ini. Silakan lakukan pengisian lagi besok.")
                            .setPositiveButton("Kembali") { _, _ -> finish() }
                            .setCancelable(false)
                            .show()
                        return@launch
                    }

                    // Lanjutkan dengan sesi yang aktif
                    updateSessionInfoInToolbar()
                    setupFormSession()
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking routine status", e)
                    // Tampilkan dialog pemilihan jika terjadi error
                    showSessionSelectionDialog()
                }
            }
        } else {
            // Setup untuk deteksi singkat
            setupFormSession()
        }

        // Observe viewmodel state
        observeViewModelState()

        // Back button handling
        onBackPressedDispatcher.addCallback(this) {
            showBackConfirmationDialog()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    // Metode terpisah untuk setup form session agar kode lebih rapi
    private fun setupFormSession() {
        lifecycleScope.launch {
            // Reset session dan mulai baru untuk memastikan konsistensi
            formSessionManager.resetSession()
            formSessionManager.startSession()
            formSessionManager.saveDetectionType(detectionType)
            Log.d(TAG, "Form session reset and started with type: $detectionType")

            // Setup ViewPager2
            setupViewPager()

            // Mulai dari halaman pertama
            binding.fragmentContainer.currentItem = 0
            updateProgressIndicator(1)
        }
    }

    private fun setupViewPager() {
        // Menyiapkan ViewPager2 dan adapter
        val adapter = FormAnxietyAdapter(this) // Adapter untuk mengelola fragment
        binding.fragmentContainer.adapter = adapter // Menetapkan adapter ke ViewPager2
        binding.fragmentContainer.isUserInputEnabled = false

        // Menambahkan callback untuk mengupdate progress indicator
        binding.fragmentContainer.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateProgressIndicator(position + 1) // Update indikator berdasarkan posisi
            }
        })
    }

    private fun handleDetectionTypeFlow() {
        // Cek jenis deteksi
        if (detectionType == "QUICK") {
            lifecycleScope.launch {
                // Step 1: Reset dan mulai dari awal untuk deteksi cepat
                formSessionManager.resetSession()
                formSessionManager.startSession()
                formSessionManager.saveDetectionType("QUICK")

                // Step 2: Persiapkan UI
                setupViewPager()

                // Step 3: Mulai dari fragment pertama
                binding.fragmentContainer.currentItem = 0

                // Step 4: Update indikator
                updateProgressIndicator(1)

                // Step 5: Log untuk debugging
                Log.d(TAG, "handleDetectionTypeFlow: Memulai form QUICK")
            }
        } else {
            // Deteksi rutin
            lifecycleScope.launch {
                val isSessionActive = routineSessionManager.isSessionStillActive()

                if (!isSessionActive) {
                    // Belum ada sesi aktif, tampilkan dialog pemilihan durasi
                    Log.d(TAG, "handleDetectionTypeFlow: Tidak ada sesi rutin aktif, tampilkan dialog pemilihan durasi")
                    showSessionSelectionDialog()
                } else {
                    // Sesi sudah aktif, cek apakah sudah mengisi hari ini
                    val hasCompletedToday = routineSessionManager.hasCompletedFormToday()
                    if (hasCompletedToday) {
                        Toast.makeText(
                            this@FormAnxietyActivity,
                            "Anda sudah mengisi form deteksi kecemasan hari ini",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.d(TAG, "handleDetectionTypeFlow: Sudah mengisi form hari ini, keluar")
                        finish()
                        return@launch
                    }

                    // Update judul untuk menampilkan info sesi
                    updateSessionInfoInToolbar()

                    // Step 1: Reset dan mulai dari awal
                    formSessionManager.resetSession()
                    formSessionManager.startSession()
                    formSessionManager.saveDetectionType("ROUTINE")

                    // Step 2: Persiapkan UI
                    setupViewPager()

                    // Step 3: Mulai dari fragment pertama
                    binding.fragmentContainer.currentItem = 0

                    // Step 4: Update indikator
                    updateProgressIndicator(1)

                    // Step 5: Log untuk debugging
                    Log.d(TAG, "handleDetectionTypeFlow: Memulai form ROUTINE untuk sesi aktif")
                }
            }
        }
    }

//    private fun handleQuickDetectionFlow() {
//        lifecycleScope.launch {
//            val isRoutineSessionActive = routineSessionManager.isSessionStillActive()
//
//            if (isRoutineSessionActive) {
//                // Ada sesi rutin aktif, cek apakah sudah mengisi hari ini
//                val hasCompletedToday = routineSessionManager.hasCompletedFormToday()
//
//                if (hasCompletedToday) {
//                    // Sudah mengisi form rutin hari ini, lanjutkan dengan deteksi singkat biasa
//                    lifecycleScope.launch {
//                        formSessionManager.saveDetectionType("QUICK")
//                    }
//                    checkSessionStatusAndNavigate()
//                } else {
//                    // Belum mengisi form rutin hari ini, tanyakan apakah ingin digabung
//                    showRoutineConflictDialog()
//                }
//            } else {
//                // Tidak ada sesi rutin aktif, lanjutkan dengan deteksi singkat biasa
//                lifecycleScope.launch {
//                    formSessionManager.saveDetectionType("QUICK")
//                }
//                checkSessionStatusAndNavigate()
//            }
//        }
//    }
//
//    private fun handleRoutineDetectionFlow() {
//        // Ambil data routine active dari repository
//        viewModel.checkActiveRoutineSession()
//
//        lifecycleScope.launch {
//            val isSessionActive = routineSessionManager.isSessionStillActive()
//
//            if (!isSessionActive) {
//                // Belum ada sesi aktif, tampilkan dialog pemilihan durasi
//                showSessionSelectionDialog()
//            } else {
//                // Sesi sudah aktif, cek apakah sudah mengisi hari ini
//                val hasCompletedToday = routineSessionManager.hasCompletedFormToday()
//                if (hasCompletedToday) {
//                    Toast.makeText(
//                        this@FormAnxietyActivity,
//                        "Anda sudah mengisi form deteksi kecemasan hari ini",
//                        Toast.LENGTH_LONG
//                    ).show()
//                    finish()
//                    return@launch
//                }
//
//                // Update judul untuk menampilkan info sesi
//                updateSessionInfoInToolbar()
//                checkSessionStatusAndNavigate()
//            }
//        }
//    }

//    private fun checkSessionStatusAndNavigate() {
//        lifecycleScope.launch {
//            val isSessionActive = formSessionManager.isSessionActive.first()
//            if (!isSessionActive) {
//                // Jika tidak ada sesi aktif, buat sesi baru
//                formSessionManager.startSession()
//                binding.fragmentContainer.currentItem = 0
//            } else {
//                // Jika sesi ada, periksa apakah status completed dan ada data
//                val currentStep = formSessionManager.currentStep.first()
//
//                // Cek jika sudah di status completed
//                if (currentStep == "gad_completed") {
//                    // Verifikasi bahwa data benar-benar ada sebelum navigasi
//                    var hasAllData = true
//                    var totalScore = 0
//                    val gadAnswers = ArrayList<Int>()
//
//                    // Verifikasi data emosi dan aktivitas
//                    val emotion = formSessionManager.getEmotion()
//                    val activity = formSessionManager.getActivity()
//
//                    if (emotion.isEmpty() || activity.isEmpty()) {
//                        hasAllData = false
//                    }
//
//                    // Verifikasi semua jawaban GAD
//                    for (i in 0..6) {
//                        val answer = formSessionManager.getGadAnswer(i)
//                        if (answer < 0) {
//                            hasAllData = false
//                            break
//                        } else {
//                            totalScore += answer
//                            gadAnswers.add(answer)
//                        }
//                    }
//
//                    if (hasAllData) {
//                        // Navigasi ke layar hasil dengan data lengkap
//                        val intent = Intent(this@FormAnxietyActivity, HasilAnxietyShortActivity::class.java)
//                        intent.putExtra("TOTAL_SCORE", totalScore)
//                        intent.putExtra("EMOTION", emotion)
//                        intent.putExtra("ACTIVITY", activity)
//                        intent.putIntegerArrayListExtra("GAD_ANSWERS", gadAnswers)
//                        startActivity(intent)
//                        finish()
//                    } else {
//                        // Reset sesi dan mulai ulang karena data tidak lengkap
//                        Log.d(TAG, "Data tidak lengkap, memulai sesi baru")
//                        formSessionManager.resetSession()
//                        formSessionManager.startSession()
//                        binding.fragmentContainer.currentItem = 0
//                    }
//                } else {
//                    // Lanjutkan dari langkah terakhir
//                    resumeFromLastStep()
//                }
//            }
//        }
//    }

    private fun observeViewModelState() {
        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe error messages
        viewModel.errorMessage.observe(this) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }

        // Observe routine session info updates
        viewModel.routineSessionInfo.observe(this) { info ->
            // Update toolbar with session info
            updateToolbarWithSessionInfo(info)
        }

        // Observe navigation to result
        viewModel.navigateToResult.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                // Navigate to result screen
                val intent = Intent(this, HasilAnxietyShortActivity::class.java)
                startActivity(intent)
                viewModel.resetNavigationState()
            }
        }
    }

    private fun updateToolbarWithSessionInfo(info: Triple<String, Int, Int>) {
        val sessionType = when(info.first) {
            "1_WEEK" -> "1 Minggu"
            "2_WEEKS" -> "2 Minggu"
            "1_MONTH" -> "1 Bulan"
            else -> info.first
        }

        supportActionBar?.title = "Deteksi Rutin"
        supportActionBar?.subtitle = "Sesi $sessionType - Hari ${info.second} dari ${info.third}"
    }

    private fun showRoutineConflictDialog() {
        AlertDialog.Builder(this)
            .setTitle("Deteksi Rutin Aktif")
            .setMessage("Anda saat ini memilih form deteksi singkat, dan Anda sudah terdaftar deteksi rutin. Form deteksi singkat ini akan masuk kedalam penjadwalan rutin harian deteksi rutin. Apakah Anda yakin?")
            .setPositiveButton("Ya, Lanjutkan") { _, _ ->
                // Ubah tipe deteksi menjadi rutin agar masuk ke penjadwalan
                detectionType = "ROUTINE"
                lifecycleScope.launch {
                    formSessionManager.resetSession()
                    formSessionManager.startSession()
                    formSessionManager.saveDetectionType("ROUTINE")
                    Log.d(TAG, "Changed detection type to ROUTINE after conflict dialog")

                    // Update UI jika diperlukan
                    updateSessionInfoInToolbar()

                    // Mulai dari fragment pertama
                    binding.fragmentContainer.currentItem = 0
                    updateProgressIndicator(1)
                }
            }
            .setNegativeButton("Tidak, Kembali") { _, _ ->
                // Reset form dan kembali
                lifecycleScope.launch {
                    formSessionManager.resetSession()
                    finish()
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun checkSessionStatusAndNavigate() {
        lifecycleScope.launch {
            // Step 1: Reset session dan buat baru untuk memastikan bersih
            formSessionManager.resetSession()
            formSessionManager.startSession()

            // Step 2: Refresh tipe deteksi untuk memastikan konsistensi
            formSessionManager.saveDetectionType(detectionType)

            // Step 3: Selalu mulai dari halaman pertama
            binding.fragmentContainer.currentItem = 0

            // Step 4: Update indikator progres
            updateProgressIndicator(1)

            // Step 5: Log untuk debugging
            Log.d(TAG, "checkSessionStatusAndNavigate: Memulai form baru dengan tipe $detectionType")
        }
    }

    private fun resumeFromLastStep() {
        lifecycleScope.launch {
            val currentStep = formSessionManager.currentStep.first()
            Log.d(TAG, "Resuming from step: $currentStep")

            when {
                currentStep == "permission" -> binding.fragmentContainer.currentItem = 0
                currentStep == "emotion" -> binding.fragmentContainer.currentItem = 1
                currentStep == "activity" -> binding.fragmentContainer.currentItem = 2
                currentStep.startsWith("gad_") -> {
                    // Ekstrak nomor pertanyaan GAD dari string "gad_X"
                    val questionNumberString = currentStep.substringAfter("gad_")
                    if (questionNumberString == "completed") {
                        // Hanya navigasi ke hasil jika semua skor GAD ada
                        var allAnswersExist = true
                        for (i in 0..6) {
                            val answer = formSessionManager.getGadAnswer(i)
                            if (answer < 0) {
                                allAnswersExist = false
                                break
                            }
                        }

                        if (allAnswersExist) {
                            val intent = Intent(this@FormAnxietyActivity, HasilAnxietyShortActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            // Jika tidak semua jawaban ada, mulai dari awal
                            binding.fragmentContainer.currentItem = 0
                        }
                    } else {
                        try {
                            val questionNumber = questionNumberString.toInt()
                            // Pertanyaan GAD dimulai dari indeks 3 di ViewPager
                            binding.fragmentContainer.currentItem = 2 + questionNumber
                        } catch (e: NumberFormatException) {
                            // Default ke pertanyaan GAD pertama jika error
                            binding.fragmentContainer.currentItem = 3
                        }
                    }
                }
                else -> binding.fragmentContainer.currentItem = 0
            }

            // Update progress indicator
            updateProgressIndicator(binding.fragmentContainer.currentItem + 1)
        }
    }

    private fun showSessionSelectionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_session_selection, null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.rgSessionType)

        Log.d(TAG, "Showing session selection dialog")

        // Pastikan ada opsi terpilih default
        radioGroup.check(R.id.rbOneWeek)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Pilih Durasi Sesi")
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Mulai Sesi") { _, _ ->
                val selectedSessionType = when(radioGroup.checkedRadioButtonId) {
                    R.id.rbOneWeek -> "1_WEEK"
                    R.id.rbTwoWeeks -> "2_WEEKS"
                    R.id.rbOneMonth -> "1_MONTH"
                    else -> "1_WEEK" // Default ke 1 minggu
                }

                Log.d(TAG, "Selected session type: $selectedSessionType")

                lifecycleScope.launch {
                    try {
                        // Ambil user ID saat ini
                        val userId = firebaseService.getCurrentUserId()
                        if (userId.isNullOrEmpty()) {
                            Toast.makeText(this@FormAnxietyActivity,
                                "Anda belum login",
                                Toast.LENGTH_SHORT).show()
                            finish()
                            return@launch
                        }

                        // Simpan ke local storage dengan user ID
                        routineSessionManager.startNewSession(selectedSessionType, userId)
                        Log.d(TAG, "Started local session of type: $selectedSessionType for user: $userId")

                        // Schedule reminders dengan AlarmManager
                        val notificationManager = NotificationSchedulerManager(this@FormAnxietyActivity)
                        notificationManager.scheduleRoutineFormRemindersWithAlarm(userId)

                        // Lanjutkan setup form
                        formSessionManager.resetSession()
                        formSessionManager.startSession()
                        formSessionManager.saveDetectionType("ROUTINE")

                        // Mulai dari fragment pertama
                        setupViewPager()
                        binding.fragmentContainer.currentItem = 0
                        updateProgressIndicator(1)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting routine session", e)
                        Toast.makeText(this@FormAnxietyActivity,
                            "Gagal memulai sesi rutin: ${e.message}",
                            Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Batal") { _, _ ->
                // Kembali ke deteksi singkat jika batal
                detectionType = "QUICK"
                lifecycleScope.launch {
                    formSessionManager.saveDetectionType("QUICK")
                }
                Toast.makeText(this, "Mode deteksi diubah ke Deteksi Singkat", Toast.LENGTH_SHORT).show()
                finish()
            }
            .create()

        dialog.show()
    }

    private fun updateSessionInfoInToolbar() {
        lifecycleScope.launch {
            val sessionType = routineSessionManager.getSessionTypeDisplay()
            val currentDay = routineSessionManager.getCurrentSessionDay()
            val totalDays = routineSessionManager.getSessionDurationInDays()

            supportActionBar?.title = "Deteksi Rutin"
            supportActionBar?.subtitle = "Sesi $sessionType - Hari $currentDay dari $totalDays"
        }
    }

    // Fungsi untuk memperbarui progress indicator
    fun updateProgressIndicator(currentPage: Int) {
        val progressIndicator = binding.llProgressIndicator
        for (i in 0 until progressIndicator.childCount) {
            val view = progressIndicator.getChildAt(i)
            view.setBackgroundResource(
                if (i < currentPage) R.drawable.round_indicator_active
                else R.drawable.round_indicator_inactive
            )
        }
    }

    private fun showBackConfirmationDialog() {
        // Menampilkan dialog konfirmasi apakah yakin ingin keluar
        AlertDialog.Builder(this)
            .setMessage("Apakah Anda yakin ingin keluar? Semua data yang belum disimpan akan hilang.")
            .setPositiveButton("Ya") { _, _ ->
                // Menghapus sesi di DataStore jika keluar
                lifecycleScope.launch {
                    formSessionManager.resetSession()
                }
                finish() // Menutup Activity
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun hideSystemUI() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // Fungsi untuk navigasi langsung ke halaman GAD tertentu
    // Fungsi untuk navigasi langsung ke halaman GAD tertentu
    fun navigateToGadQuestion(questionNumber: Int) {
        try {
            // Index pertanyaan GAD di ViewPager (GAD dimulai dari index 3)
            val pageIndex = 3 + questionNumber

            Log.d(TAG, "navigateToGadQuestion: Pindah ke pertanyaan $questionNumber (position $pageIndex)")

            // Gunakan runOnUiThread untuk memastikan perubahan UI aman
            runOnUiThread {
                if (binding.fragmentContainer.currentItem != pageIndex) {
                    binding.fragmentContainer.currentItem = pageIndex

                    // Update progress indicator
                    updateProgressIndicator(pageIndex + 1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saat navigasi ke pertanyaan GAD", e)
        }
    }

    // Fungsi untuk check apakah routine session aktif (dapat dipanggil dari fragment)
    fun isRoutineSessionActive(): Boolean {
        return viewModel.activeRoutineDetection.value != null
    }

    // Fungsi untuk mendapatkan tipe deteksi saat ini
    fun getCurrentDetectionType(): String {
        return detectionType
    }

    // Metode untuk debugging DataStore - untuk debugging saja
    private fun debugDataStore() {
        viewModel.getDetectionTypeInfo()
    }

    override fun onResume() {
        super.onResume()
        // Check kembali active routine session setiap kali activity di-resume
        if (detectionType == "ROUTINE") {
            viewModel.checkActiveRoutineSession()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        private const val TAG = "FormAnxietyActivity"
    }
}
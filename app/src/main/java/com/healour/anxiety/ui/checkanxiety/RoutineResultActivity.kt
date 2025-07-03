package com.healour.anxiety.ui.checkanxiety

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkManager
import com.healour.anxiety.R
import com.healour.anxiety.core.knn.PersonalPredictionService
import com.healour.anxiety.data.firebase.FirebaseService
import com.healour.anxiety.data.model.RoutineDetectionModel
import com.healour.anxiety.databinding.ActivityRoutineResultBinding
import com.healour.anxiety.receivers.PredictionAlarmReceiver
import com.healour.anxiety.ui.checkanxiety.adapter.DailyDetectionAdapter
import com.healour.anxiety.utils.NotificationSchedulerManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Activity for displaying routine anxiety detection results and predictions.
 * This version includes functionality for scheduling prediction notifications.
 */
class RoutineResultActivity : AppCompatActivity() {
    private var _binding: ActivityRoutineResultBinding? = null
    private val binding get() = _binding!!
    private lateinit var firebaseService: FirebaseService
    private lateinit var predictionService: PersonalPredictionService
    private lateinit var notificationManager: NotificationSchedulerManager

    // Flag to track if notifications are currently scheduled
    private var isPredictionNotificationsActive = false

    companion object {
        private const val TAG = "RoutineResultActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityRoutineResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left + v.paddingLeft, systemBars.top+v.paddingTop,
                systemBars.right +v.paddingRight, systemBars.bottom+v.paddingBottom)
            insets
        }

        // Initialize services
        firebaseService = FirebaseService()
        predictionService = PersonalPredictionService(this)
        notificationManager = NotificationSchedulerManager(this)

        // Check if notifications are already scheduled
        isPredictionNotificationsActive = notificationManager.isPredictionNotificationsScheduled()

        // Setup back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Setup notification toggle button
        setupNotificationButton()

        // Load data
        loadLatestCompletedRoutineDetection()
        // Tambahkan tombol test di UI
//        binding.btnTestNotification?.setOnClickListener {
//            testImmediateNotification()
//        }
//        checkWorkStatus()
    }

    /**
     * Sets up the notification toggle button
     */
    private fun setupNotificationButton() {
        binding.btnToggleNotifications?.let { button ->
            // Set initial button state
            updateNotificationButtonState(button)

            // Set click listener
            button.setOnClickListener {
                if (isPredictionNotificationsActive) {
                    // Cancel notifications using AlarmManager
                    notificationManager.cancelAlarmNotifications()
                    isPredictionNotificationsActive = false
                    Toast.makeText(this, "Notifikasi prediksi dinonaktifkan", Toast.LENGTH_SHORT).show()
                } else {
                    // Request exact alarm permission for Android 12+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        if (!alarmManager.canScheduleExactAlarms()) {
                            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                            Toast.makeText(this, "Izinkan pengaturan alarm untuk notifikasi", Toast.LENGTH_LONG).show()
                            return@setOnClickListener
                        }
                    }

                    // Schedule notifications using AlarmManager
                    val success = notificationManager.scheduleDailyPredictionNotificationWithAlarm(8, 0)
                    isPredictionNotificationsActive = success

                    if (success) {
                        Toast.makeText(this, "Notifikasi prediksi diaktifkan", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Gagal mengaktifkan notifikasi", Toast.LENGTH_SHORT).show()
                    }
                }

                // Update button appearance
                updateNotificationButtonState(button)
            }
        }
    }

    /**
     * Updates notification button text and appearance based on current state
     */
    private fun updateNotificationButtonState(button: MaterialButton) {
        if (isPredictionNotificationsActive) {
            button.text = "Berhenti Notifikasi Prediksi"
            button.icon = ContextCompat.getDrawable(this, R.drawable.ic_notifications_off)
            button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.gray500)
        } else {
            button.text = "Aktifkan Notifikasi Prediksi"
            button.icon = ContextCompat.getDrawable(this, R.drawable.ic_notifications)
            button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.bluePrimary)
        }
    }

    // Tambahkan di RoutineResultActivity
    private fun checkWorkStatus() {
        WorkManager.getInstance(this)
            .getWorkInfosForUniqueWork(NotificationSchedulerManager.WORK_NAME_PREDICTION)
            .get()
            .forEach { workInfo ->
                Log.d("WorkStatus", "State: ${workInfo.state}, ID: ${workInfo.id}")
            }
    }
    // Di RoutineResultActivity, tambah fungsi untuk test immediate notification
    private fun testImmediateNotification() {
        // Test immediate notification using AlarmManager
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, PredictionAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            9999, // Different request code for testing
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set alarm to trigger in 10 seconds
        val triggerTime = System.currentTimeMillis() + 10000

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }

        Toast.makeText(this, "Test notification scheduled in 10 seconds", Toast.LENGTH_SHORT).show()
    }

    private fun loadLatestCompletedRoutineDetection() {
        lifecycleScope.launch {
            try {
                val userId = firebaseService.getCurrentUserId()
                if (userId == null) {
                    showError("User tidak login")
                    return@launch
                }

                // Get all routine detections
                val result = firebaseService.getRoutineDetections(userId)
                if (result.isFailure) {
                    showError("Gagal mengambil data: ${result.exceptionOrNull()?.message}")
                    return@launch
                }

                val routineDetections = result.getOrNull()
                if (routineDetections.isNullOrEmpty()) {
                    showNoData()
                    return@launch
                }

                // Filter for inactive (completed) documents
                val completedDetections = routineDetections.filter { !it.second.aktif }

                if (completedDetections.isEmpty()) {
                    Log.d(TAG, "Tidak ada dokumen dengan status non-aktif")
                    showNoData()
                    return@launch
                }

                // Sort by document ID number (deteksi_rutin_X)
                val latestDetection = completedDetections.maxByOrNull {
                    it.first.substringAfterLast("_").toIntOrNull() ?: 0
                }

                if (latestDetection == null) {
                    showNoData()
                    return@launch
                }

                // Display latest routine detection data
                Log.d(TAG, "Menampilkan dokumen terbaru: ${latestDetection.first}")
                displayRoutineDetection(latestDetection.first, latestDetection.second)

            } catch (e: Exception) {
                Log.e(TAG, "Error loading data", e)
                showError("Terjadi kesalahan: ${e.message}")
            }
        }
    }

    private fun displayRoutineDetection(docId: String, routineDetection: RoutineDetectionModel) {
        // Format period
        val periode = when(routineDetection.periode) {
            "1_WEEK" -> "1 Minggu"
            "2_WEEKS" -> "2 Minggu"
            "1_MONTH" -> "1 Bulan"
            else -> routineDetection.periode
        }

        // Format dates
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        val tanggalMulai = dateFormat.format(routineDetection.tanggalMulai.toDate())
        val tanggalSelesai = dateFormat.format(routineDetection.tanggalSelesai.toDate())

        // Display basic information
        binding.tvPeriodic.text = "Periode Sesi: $periode"
        binding.tvTglMulai.text = "Tanggal Mulai: $tanggalMulai"
        binding.tvTglBerakhir.text = "Tanggal Berakhir: $tanggalSelesai"

        // Display lowest and highest scores
        binding.tvSkorRendah.text = "Berdasarkan Deteksi yang Anda Isikan Secara Rutin, " +
                "Skor ${routineDetection.skorRendah} adalah Skor Terendah yang Muncul di Hari ${routineDetection.hariSkorRendah}"

        binding.tvSkorTinggi.text = "Berdasarkan Deteksi yang Anda Isikan Secara Rutin, " +
                "Skor ${routineDetection.skorTinggi} adalah Skor Tertinggi yang Muncul di Hari ${routineDetection.hariSkorTinggi}"

        // Display RecyclerView if there is daily data
        if (routineDetection.deteksiHarian.isNotEmpty()) {
            setupRecyclerView(routineDetection)
        } else {
            binding.recyclerViewDeteksiHarian.visibility = View.GONE
        }

        // Display severity summary for historical data
        displaySeveritySummary(routineDetection)

        // Generate and display predictions
        displayPredictions(routineDetection)

        // Make notification button visible since we have data to create notifications
        binding.btnToggleNotifications?.visibility = View.VISIBLE
    }

    private fun setupRecyclerView(routineDetection: RoutineDetectionModel) {
        binding.recyclerViewDeteksiHarian.layoutManager = LinearLayoutManager(this)

        // Convert Map<String, DailyDetectionData> to List<Pair<String, DailyDetectionData>>
        val dailyDataList = routineDetection.deteksiHarian.entries
            .map { Pair(it.key, it.value) }
            .sortedBy { it.first.toIntOrNull() ?: 0 }

        val adapter = DailyDetectionAdapter(dailyDataList)
        binding.recyclerViewDeteksiHarian.adapter = adapter

        // Ensure RecyclerView scrolls properly within ScrollView
        binding.recyclerViewDeteksiHarian.isNestedScrollingEnabled = false
    }

    private fun displaySeveritySummary(routineDetection: RoutineDetectionModel) {
        // Create severity summary by day with color coding
        val severitySummary = StringBuilder()
        severitySummary.append("<b>Riwayat Tingkat Kecemasan:</b> ")

        routineDetection.deteksiHarian.entries
            .sortedBy { it.key.toIntOrNull() ?: 0 }
            .forEachIndexed { index, entry ->
                val hari = getDayName(entry.value.tanggal.toDate())
                val severity = entry.value.severity
                val colorCode = getSeverityColorHex(severity)

                if (index > 0) severitySummary.append(", ")
                severitySummary.append("<font color='$colorCode'>$hari: $severity</font>")
            }

        binding.tvShowAllDaySeverity.text = android.text.Html.fromHtml(
            severitySummary.toString(),
            android.text.Html.FROM_HTML_MODE_COMPACT
        )
    }

    private fun displayPredictions(routineDetection: RoutineDetectionModel) {
        try {
            // Convert map to sorted list of DailyDetectionData
            val dailyDataList = routineDetection.deteksiHarian.entries
                .map { it.value }
                .sortedBy { it.tanggal }

            // Get the last day number
            val lastDayNumber = routineDetection.deteksiHarian.keys
                .mapNotNull { it.toIntOrNull() }
                .maxOrNull() ?: 0

            // Log data points being used for prediction
            Log.d(TAG, "Using ${dailyDataList.size} personal data points for prediction")

            // Generate detailed predictions using only personal data
            val predictionDetails = predictionService.predictNextSevenDaysDetail(
                lastDayNumber,
                dailyDataList
            )

            if (predictionDetails.isEmpty()) {
                binding.tvPredictedSeverity.text = "Prediksi 7 Hari: Data historis tidak cukup untuk membuat prediksi."
                binding.btnToggleNotifications?.visibility = View.GONE // Hide button if no predictions
                return
            }

            // Format for each day with color coding by severity
            val predictionHtml = StringBuilder()
            predictionHtml.append("<b>Prediksi 7 Hari ke Depan (Berdasarkan Data Personal Anda):</b><br>")

            predictionDetails.forEachIndexed { index, detail ->
                // Add separator for days
                if (index > 0) predictionHtml.append("<br>")

                // Add confidence indicator
                val confidenceIndicator = when {
                    detail.confidence >= 0.8 -> "🟢" // High confidence
                    detail.confidence >= 0.5 -> "🟡" // Medium confidence
                    else -> "🔴" // Low confidence
                }

                // Get color based on severity
                val severityColor = getSeverityColorHex(detail.predictedSeverity)

                // Add day header with colored severity
                predictionHtml.append("<b>${index+1}. ${detail.dayOfWeek} $confidenceIndicator:</b> ")
                predictionHtml.append("Prediksi: <font color='$severityColor'>${detail.predictedSeverity}</font> (Skor: ${detail.predictedGadScore})<br>")

                // Add personalized description
                predictionHtml.append(detail.getReadableDescription())

                // Add recommendation
                predictionHtml.append("<br><i>Rekomendasi:</i> ${detail.getRecommendedActivity()}")
            }

            // Display predictions in HTML format
            binding.tvPredictedSeverity.text = android.text.Html.fromHtml(
                predictionHtml.toString(),
                android.text.Html.FROM_HTML_MODE_COMPACT
            )

            // Log predictions for debugging
            Log.d(TAG, "Generated ${predictionDetails.size} predictions using personal data")
            predictionDetails.forEachIndexed { index, detail ->
                Log.d(TAG, "$index: ${detail.dayOfWeek} - ${detail.predictedSeverity} (confidence: ${detail.confidence})")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying predictions: ${e.message}", e)
            binding.tvPredictedSeverity.text = "Prediksi 7 Hari: Terjadi kesalahan dalam pembuatan prediksi."
            binding.btnToggleNotifications?.visibility = View.GONE // Hide button on error
        }
    }

    /**
     * Returns color hex code based on severity level
     */
    private fun getSeverityColorHex(severity: String): String {
        return when (severity) {
            "Minimal" -> "#4CAF50" // Green
            "Ringan" -> "#2196F3"  // Blue
            "Sedang" -> "#FF9800"  // Orange
            "Parah" -> "#F44336"   // Red
            else -> "#757575"      // Gray
        }
    }

    private fun getDayName(date: Date): String {
        val dayFormat = SimpleDateFormat("EEEE", Locale("id", "ID"))
        return dayFormat.format(date)
    }

    private fun showNoData() {
        binding.tvPeriodic.text = "Tidak ada data deteksi rutin yang selesai"
        binding.tvTglMulai.visibility = View.GONE
        binding.tvTglBerakhir.visibility = View.GONE
        binding.recyclerViewDeteksiHarian.visibility = View.GONE

        binding.tvSkorRendah.visibility = View.GONE
        binding.tvSkorTinggi.visibility = View.GONE
        binding.tvShowAllDaySeverity.visibility = View.GONE
        binding.tvPredictedSeverity.visibility = View.GONE
        binding.btnToggleNotifications?.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.tvPeriodic.text = "Terjadi kesalahan: $message"
        binding.tvTglMulai.visibility = View.GONE
        binding.tvTglBerakhir.visibility = View.GONE
        binding.recyclerViewDeteksiHarian.visibility = View.GONE

        binding.tvSkorRendah.visibility = View.GONE
        binding.tvSkorTinggi.visibility = View.GONE
        binding.tvShowAllDaySeverity.visibility = View.GONE
        binding.tvPredictedSeverity.visibility = View.GONE
        binding.btnToggleNotifications?.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
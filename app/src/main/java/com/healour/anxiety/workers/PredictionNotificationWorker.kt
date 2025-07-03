package com.healour.anxiety.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.healour.anxiety.MainActivity
import com.healour.anxiety.R
import com.healour.anxiety.core.knn.PersonalPredictionService
import com.healour.anxiety.data.firebase.FirebaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Worker that sends notifications with anxiety predictions based on user's personal data.
 * This notification includes prediction for the current day, showing expected anxiety level,
 * emotions, activities, and recommendations.
 */
class PredictionNotificationWorker(
    private val appContext: Context,
    private val workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "PREDICTION_NOTIFICATION_CHANNEL"
        const val NOTIFICATION_ID = 3
        const val TAG = "PredictionNotifWorker"

        // Extra data keys for input
        const val KEY_DOCUMENT_ID = "documentId"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {


            Log.d(TAG, "========= PREDICTION NOTIFICATION WORKER STARTED =========")
            Log.d(TAG, "Worker executed at: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            Log.d(TAG, "Worker ID: ${workerParams.id}")
            // Check if notification was already shown today to prevent duplicates
            val prefs = appContext.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
            val lastNotificationDate = prefs.getString("last_prediction_notification", "")
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            if (lastNotificationDate == today) {
                Log.d(TAG, "Notification already shown today, skipping")
                return@withContext Result.success()
            }

            // Create notification channel first
            createNotificationChannel()

            // Get current day of week
            val calendar = Calendar.getInstance()
            val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
            val currentDayName = getDayName(currentDayOfWeek)

            Log.d(TAG, "Current day: $currentDayOfWeek ($currentDayName)")

            // Get prediction for today
            val prediction = getTodayPrediction(currentDayOfWeek)
            if (prediction == null) {
                Log.e(TAG, "No prediction available for today")
                return@withContext Result.failure()
            }

            Log.d(TAG, "Prediction found: ${prediction.predictedSeverity}")

            // Create notification
            val notificationText = buildNotificationText(prediction)

            // Show notification
            showNotification(notificationText)

            // Save the date after showing notification successfully
            prefs.edit().putString("last_prediction_notification", today).apply()

            Log.d(TAG, "========= PREDICTION NOTIFICATION WORKER COMPLETED =========")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in prediction notification worker", e)
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun showNotification(text: String) {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_about)
            .setContentTitle("Prediksi Kecemasan Hari Ini")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        Log.d(TAG, "Notification posted with ID: $NOTIFICATION_ID")
    }

    /**
     * Creates formatted notification text from prediction details
     */
    private fun buildNotificationText(prediction: PersonalPredictionService.PredictionDetail): String {
        // Format based on anxiety level
        val severityText = when (prediction.predictedSeverity) {
            "Minimal" -> "tingkat kecemasan minimal"
            "Ringan" -> "tingkat kecemasan ringan"
            "Sedang" -> "tingkat kecemasan sedang"
            "Parah" -> "tingkat kecemasan parah"
            else -> "tingkat kecemasan yang tidak dapat diprediksi"
        }

        return "Hari ini Anda diperkirakan akan mengalami $severityText " +
                "dengan emosi ${prediction.suggestedEmosi} saat melakukan aktivitas ${prediction.suggestedAktivitas}.\n\n" +
                "Rekomendasi: ${prediction.getRecommendedActivity()}"
    }

    /**
     * Gets prediction for today from user's personal data
     */
    private suspend fun getTodayPrediction(currentDayOfWeek: Int): PersonalPredictionService.PredictionDetail? {
        try {
            val firebaseService = FirebaseService()
            val userId = firebaseService.getCurrentUserId() ?: return null

            // Get latest routine detection data
            val result = firebaseService.getRoutineDetections(userId)
            if (result.isFailure) {
                Log.e(TAG, "Failed to get routine detections: ${result.exceptionOrNull()?.message}")
                return null
            }

            val detections = result.getOrNull()
            if (detections.isNullOrEmpty()) {
                Log.d(TAG, "No detection data available")
                return null
            }

            // Get latest completed detection
            val latestDetection = detections
                .filter { !it.second.aktif }
                .maxByOrNull { it.first.substringAfterLast("_").toIntOrNull() ?: 0 }
                ?.second

            if (latestDetection == null) {
                Log.d(TAG, "No completed detection found")
                return null
            }

            // Convert daily data to list
            val dailyDataList = latestDetection.deteksiHarian.values.toList().sortedBy { it.tanggal }

            // Get last day number
            val lastDayNumber = latestDetection.deteksiHarian.keys
                .mapNotNull { it.toIntOrNull() }
                .maxOrNull() ?: 0

            // Create prediction service and get predictions
            val predictionService = PersonalPredictionService(appContext)
            val predictions = predictionService.predictNextSevenDaysDetail(lastDayNumber, dailyDataList)

            if (predictions.isEmpty()) {
                Log.d(TAG, "No predictions generated")
                return null
            }

            // Find prediction for current day of week
            return predictions.find { getDayOfWeek(it.dayOfWeek) == currentDayOfWeek }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving prediction", e)
            return null
        }
    }

    /**
     * Creates notification channel for Android O and above
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Prediksi Kecemasan"
            val descriptionText = "Notifikasi untuk prediksi tingkat kecemasan harian"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Gets day name from day of week index (0-6)
     */
    private fun getDayName(dayOfWeek: Int): String {
        val days = arrayOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
        return days[dayOfWeek % 7]
    }

    /**
     * Gets day of week index (0-6) from day name
     */
    private fun getDayOfWeek(dayName: String): Int {
        return when (dayName.lowercase(Locale.getDefault())) {
            "minggu" -> 0
            "senin" -> 1
            "selasa" -> 2
            "rabu" -> 3
            "kamis" -> 4
            "jumat" -> 5
            "sabtu" -> 6
            else -> -1
        }
    }
}
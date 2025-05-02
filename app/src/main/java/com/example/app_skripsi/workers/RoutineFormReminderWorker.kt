package com.example.app_skripsi.workers

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
import com.example.app_skripsi.MainActivity
import com.example.app_skripsi.R
import com.example.app_skripsi.data.local.RoutineSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Worker that sends reminders to fill out routine anxiety detection forms
 * when there is an active session and the user hasn't completed the form for today.
 *
 * This worker can be scheduled to run up to 3 times a day at different times
 * to ensure the user doesn't miss filling out the form.
 */
class RoutineFormReminderWorker(
    private val appContext: Context,
    private val workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "ROUTINE_FORM_REMINDER_CHANNEL"
        const val NOTIFICATION_ID_BASE = 100 // Use different ID for each notification time
        const val TAG = "RoutineFormReminder"

        // Input data keys
        const val KEY_REMINDER_NUMBER = "reminderNumber" // 1, 2, or 3 for different times of day
    }

    // Get the routine session manager to check session status
    private val routineSessionManager = RoutineSessionManager(appContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting routine form reminder check")

            // Check if there's an active session
            val isSessionActive = routineSessionManager.isSessionStillActive()
            if (!isSessionActive) {
                Log.d(TAG, "No active routine session found")
                return@withContext Result.success() // Nothing to do if no active session
            }

            // Check if user already completed form for today
            val completedToday = routineSessionManager.hasCompletedFormToday()
            if (completedToday) {
                Log.d(TAG, "User already completed form today")
                return@withContext Result.success() // Nothing to do if form already completed
            }

            // Get reminder number to customize message and notification ID
            val reminderNumber = workerParams.inputData.getInt(KEY_REMINDER_NUMBER, 1)
            val notificationId = NOTIFICATION_ID_BASE + reminderNumber

            // Create notification channel
            createNotificationChannel()

            // Get session details for more personalized reminder
            val sessionType = routineSessionManager.getSessionTypeDisplay()
            val currentDay = routineSessionManager.getCurrentSessionDay()
            val totalDays = routineSessionManager.getSessionDurationInDays()

            // Create intent to open MainActivity (you might want to deep link to form activity)
            val intent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                // You can add extras here to take user directly to form
                putExtra("OPEN_FORM", true)
                putExtra("DETECTION_TYPE", "ROUTINE")
            }

            val pendingIntent = PendingIntent.getActivity(
                appContext,
                notificationId,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Create notification with reminder
            val notificationTitle = "Pengingat Deteksi Kecemasan"
            val notificationText = buildReminderText(reminderNumber, sessionType, currentDay, totalDays)

            val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_about)  // Use appropriate icon
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Higher priority for reminders
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            // Show the notification
            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, notification)

            Log.d(TAG, "Routine form reminder #$reminderNumber shown successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing routine form reminder", e)
            Result.failure()
        }
    }

    /**
     * Builds the reminder text based on reminder number and session details
     */
    private fun buildReminderText(
        reminderNumber: Int,
        sessionType: String,
        currentDay: Int,
        totalDays: Int
    ): String {
        val dayFormat = SimpleDateFormat("EEEE", Locale("id", "ID"))
        val today = dayFormat.format(Date())

        return when (reminderNumber) {
            1 -> "Anda belum mengisi form deteksi kecemasan hari ini ($today). " +
                    "Ini adalah hari ke-$currentDay dari $totalDays dalam sesi $sessionType Anda."

            2 -> "Jangan lupa mengisi form deteksi kecemasan hari ini. " +
                    "Pengisian rutin membantu analisis kecemasan menjadi lebih akurat."

            else -> "Pengingat terakhir! Anda belum mengisi form deteksi kecemasan hari ini. " +
                    "Mohon segera isi untuk kelengkapan data sesi $sessionType Anda."
        }
    }

    /**
     * Creates notification channel for Android O and above
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Pengingat Form Deteksi"
            val descriptionText = "Notifikasi pengingat untuk mengisi form deteksi kecemasan rutin"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
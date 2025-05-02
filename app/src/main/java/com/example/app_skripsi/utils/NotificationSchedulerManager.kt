package com.example.app_skripsi.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.*
import com.example.app_skripsi.receivers.PredictionAlarmReceiver
import com.example.app_skripsi.receivers.RoutineFormAlarmReceiver
import com.example.app_skripsi.workers.DailyNotificationWorker
import com.example.app_skripsi.workers.PredictionNotificationWorker
import com.example.app_skripsi.workers.RoutineFormReminderWorker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Utility class to manage scheduling of different notification types using WorkManager.
 * Provides methods to schedule and cancel notifications for anxiety predictions and
 * routine form reminders.
 */
class NotificationSchedulerManager(private val context: Context) {

    companion object {
        private const val TAG = "NotificationScheduler"

        // AlarmManager codes
        private const val ALARM_REQUEST_CODE = 1001

        // Work request tags
        const val TAG_PREDICTION_NOTIFICATION = "prediction_notification"
        const val TAG_ROUTINE_FORM_REMINDER_1 = "routine_form_reminder_1"
        const val TAG_ROUTINE_FORM_REMINDER_2 = "routine_form_reminder_2"
        const val TAG_ROUTINE_FORM_REMINDER_3 = "routine_form_reminder_3"

        // Work request names
        const val WORK_NAME_PREDICTION = "prediction_notification_work"
        const val WORK_NAME_ROUTINE_REMINDER = "routine_form_reminder_work"

        // Preference keys
        const val PREF_NOTIFICATIONS_ENABLED = "prediction_notifications_enabled"
        private const val ROUTINE_ALARM_BASE_CODE = 2000
    }

    /**
     * Schedules daily notifications using AlarmManager for more reliable execution
     */
    fun scheduleDailyPredictionNotificationWithAlarm(hour: Int = 8, minute: Int = 0): Boolean {
        return try {
            Log.d(TAG, "Scheduling with AlarmManager at $hour:$minute")

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Create intent for AlarmReceiver
            val intent = Intent(context, PredictionAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Calculate time for next alarm
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // If time has passed today, schedule for tomorrow
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            // Set exact alarm based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }

            // Save notification enabled state
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean(PREF_NOTIFICATIONS_ENABLED, true)
                .apply()

            Log.d(TAG, "AlarmManager set for: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(calendar.time)}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule with AlarmManager", e)
            false
        }
    }

    /**
     * Cancels AlarmManager notifications
     */
    fun cancelAlarmNotifications() {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, PredictionAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)

            // Save notification disabled state
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean(PREF_NOTIFICATIONS_ENABLED, false)
                .apply()

            Log.d(TAG, "AlarmManager notifications cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling alarm notifications", e)
        }
    }


    fun isPredictionNotificationsScheduled(): Boolean {
        return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getBoolean(PREF_NOTIFICATIONS_ENABLED, false)
    }


    fun scheduleDailyPredictionNotification(hour: Int = 8, minute: Int = 0): Boolean {
        return try {
            Log.d(TAG, "========= SCHEDULING PREDICTION NOTIFICATION =========")
            Log.d(TAG, "Target time: $hour:$minute")

            // Cancel any existing work first
            cancelPredictionNotifications()

            // Calculate initial delay to first notification
            val initialDelayMillis = calculateInitialDelay(hour, minute)

            // Untuk notifikasi pertama, gunakan OneTimeWorkRequest
            if (initialDelayMillis < TimeUnit.HOURS.toMillis(1)) {
                scheduleOneTimeNotification(initialDelayMillis)
            }

            // Schedule periodic work untuk hari berikutnya
            schedulePeriodicNotification(hour, minute)

            Log.d(TAG, "Successfully scheduled daily prediction notification")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule daily prediction notification", e)
            false
        }
    }

    private fun scheduleOneTimeNotification(delayMillis: Long) {
        val oneTimeRequest = OneTimeWorkRequestBuilder<PredictionNotificationWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag(TAG_PREDICTION_NOTIFICATION)
            .build()

        WorkManager.getInstance(context)
            .enqueue(oneTimeRequest)

        Log.d(TAG, "Scheduled one-time notification with delay: $delayMillis ms")
    }

    private fun schedulePeriodicNotification(hour: Int, minute: Int) {
        // Hitung delay untuk notifikasi besok
        val tomorrowDelay = calculateDelayForTomorrow(hour, minute)

        val periodicRequest = PeriodicWorkRequestBuilder<PredictionNotificationWorker>(
            24, TimeUnit.HOURS,
            15, TimeUnit.MINUTES
        )
            .setInitialDelay(tomorrowDelay, TimeUnit.MILLISECONDS)
            .addTag(TAG_PREDICTION_NOTIFICATION)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                WORK_NAME_PREDICTION,
                ExistingPeriodicWorkPolicy.REPLACE,
                periodicRequest
            )
    }

    private fun calculateDelayForTomorrow(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance()
        val targetTime = calendar.clone() as Calendar

        targetTime.set(Calendar.HOUR_OF_DAY, hour)
        targetTime.set(Calendar.MINUTE, minute)
        targetTime.set(Calendar.SECOND, 0)
        targetTime.set(Calendar.MILLISECOND, 0)

        // Selalu jadwalkan untuk besok
        targetTime.add(Calendar.DAY_OF_YEAR, 1)

        return targetTime.timeInMillis - calendar.timeInMillis
    }

    // Tambahkan method baru di NotificationSchedulerManager
    fun scheduleRoutineFormRemindersWithAlarm(userId: String): Boolean {
        return try {
            Log.d(TAG, "Scheduling routine form reminders with AlarmManager for user: $userId")

            // Cancel existing alarms dulu
            cancelRoutineFormAlarms()

            val currentTime = Calendar.getInstance()
            val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
            val currentMinute = currentTime.get(Calendar.MINUTE)

            // Schedule morning reminder (9:00 AM) - hanya jika sebelum jam 9
            if (currentHour < 9 || (currentHour == 9 && currentMinute == 0)) {
                scheduleRoutineFormAlarm(9, 0, 1, userId, false)
            } else {
                scheduleRoutineFormAlarm(9, 0, 1, userId, true)
            }

            // Schedule afternoon reminder (2:00 PM) - hanya jika sebelum jam 14
            if (currentHour < 14 || (currentHour == 14 && currentMinute == 0)) {
                scheduleRoutineFormAlarm(14, 0, 2, userId, false)
            } else {
                scheduleRoutineFormAlarm(14, 0, 2, userId, true)
            }

            // Schedule evening reminder (8:00 PM) - hanya jika sebelum jam 20
            if (currentHour < 20 || (currentHour == 20 && currentMinute == 0)) {
                scheduleRoutineFormAlarm(20, 0, 3, userId, false)
            } else {
                scheduleRoutineFormAlarm(20, 0, 3, userId, true)
            }

            Log.d(TAG, "Successfully scheduled routine form reminders")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule routine form reminders", e)
            false
        }
    }

    private fun scheduleRoutineFormAlarm(
        hour: Int,
        minute: Int,
        reminderNumber: Int,
        userId: String,
        forTomorrow: Boolean = false
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, RoutineFormAlarmReceiver::class.java).apply {
            putExtra(RoutineFormAlarmReceiver.EXTRA_REMINDER_NUMBER, reminderNumber)
            putExtra(RoutineFormAlarmReceiver.EXTRA_USER_ID, userId)
        }

        val requestCode = ROUTINE_ALARM_BASE_CODE + reminderNumber
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Jika waktu sudah lewat atau forTomorrow true, jadwalkan untuk besok
            if (forTomorrow || timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }

        val scheduledTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(calendar.timeInMillis))
        Log.d(TAG, "Scheduled routine form reminder #$reminderNumber for $scheduledTime")
    }

    fun cancelRoutineFormAlarms() {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            for (i in 1..3) {
                val intent = Intent(context, RoutineFormAlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    ROUTINE_ALARM_BASE_CODE + i,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                alarmManager.cancel(pendingIntent)
            }

            Log.d(TAG, "Cancelled all routine form alarms")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling routine form alarms", e)
        }
    }

    private fun scheduleRoutineFormReminder(
        hour: Int,
        minute: Int,
        reminderNumber: Int,
        tag: String
    ) {
        val initialDelayMillis = calculateInitialDelay(hour, minute)

        // Data for the worker
        val inputData = Data.Builder()
            .putInt(RoutineFormReminderWorker.KEY_REMINDER_NUMBER, reminderNumber)
            .build()

        // Create work request
        val reminderConstraints = Constraints.Builder()
            .build()

        val reminderWorkRequest = PeriodicWorkRequestBuilder<RoutineFormReminderWorker>(
            24, TimeUnit.HOURS // Repeat every 24 hours
        )
            .setConstraints(reminderConstraints)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(tag)
            .build()

        // Enqueue the work request with unique name based on reminder number
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "$WORK_NAME_ROUTINE_REMINDER$reminderNumber",
                ExistingPeriodicWorkPolicy.REPLACE,
                reminderWorkRequest
            )

        Log.d(TAG, "Scheduled routine form reminder #$reminderNumber at $hour:$minute")
    }

    /**
     * Cancels all prediction notifications
     */
    fun cancelPredictionNotifications() {
        try {
            WorkManager.getInstance(context)
                .cancelUniqueWork(WORK_NAME_PREDICTION)

            Log.d(TAG, "Cancelled all prediction notifications")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling prediction notifications", e)
        }
    }

    fun cancelRoutineFormReminders() {
        try {
            // Cancel all three reminders
            for (i in 1..3) {
                WorkManager.getInstance(context)
                    .cancelUniqueWork("$WORK_NAME_ROUTINE_REMINDER$i")
            }

            Log.d(TAG, "Cancelled all routine form reminders")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling routine form reminders", e)
        }
    }

    private fun calculateInitialDelay(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance()
        val targetTime = calendar.clone() as Calendar

        targetTime.set(Calendar.HOUR_OF_DAY, hour)
        targetTime.set(Calendar.MINUTE, minute)
        targetTime.set(Calendar.SECOND, 0)
        targetTime.set(Calendar.MILLISECOND, 0)

        Log.d(TAG, "Current calendar time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(calendar.time)}")
        Log.d(TAG, "Target calendar time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(targetTime.time)}")

        // Berikan grace period 5 menit
        val currentTimeMillis = calendar.timeInMillis
        val targetTimeMillis = targetTime.timeInMillis

        // Jika waktu target sudah lewat lebih dari 5 menit, jadwalkan untuk besok
        if (targetTimeMillis < currentTimeMillis - (5 * 60 * 1000)) {
            targetTime.add(Calendar.DAY_OF_YEAR, 1)
            Log.d(TAG, "Target time is in the past (more than 5 minutes), scheduling for tomorrow")
        } else if (targetTimeMillis < currentTimeMillis) {
            // Jika baru lewat kurang dari 5 menit, jadwalkan segera
            Log.d(TAG, "Target time just passed (less than 5 minutes), scheduling immediately")
            return 1000 // 1 detik delay
        }

        val delay = targetTime.timeInMillis - calendar.timeInMillis
        Log.d(TAG, "Calculated delay: $delay ms")

        return delay
    }

    fun scheduleNextRoutineReminder(reminderNumber: Int, userId: String) {
        try {
            Log.d(TAG, "Scheduling next routine reminder #$reminderNumber for user: $userId")

            // Tentukan waktu untuk alarm berikutnya berdasarkan reminder number
            val (hour, minute) = when (reminderNumber) {
                1 -> Pair(9, 0)    // Pagi
                2 -> Pair(14, 0)   // Siang
                3 -> Pair(20, 0)   // Malam
                else -> Pair(9, 0)  // Default ke pagi
            }

            // Jadwalkan untuk besok
            scheduleRoutineFormAlarmForTomorrow(hour, minute, reminderNumber, userId)

        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling next routine reminder", e)
        }
    }

    private fun scheduleRoutineFormAlarmForTomorrow(hour: Int, minute: Int, reminderNumber: Int, userId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, RoutineFormAlarmReceiver::class.java).apply {
            putExtra(RoutineFormAlarmReceiver.EXTRA_REMINDER_NUMBER, reminderNumber)
            putExtra(RoutineFormAlarmReceiver.EXTRA_USER_ID, userId)
        }

        val requestCode = ROUTINE_ALARM_BASE_CODE + reminderNumber
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Atur waktu untuk besok
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Tambahkan 1 hari
            add(Calendar.DAY_OF_YEAR, 1)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }

        Log.d(TAG, "Scheduled next routine reminder #$reminderNumber for tomorrow at $hour:$minute")
    }
}
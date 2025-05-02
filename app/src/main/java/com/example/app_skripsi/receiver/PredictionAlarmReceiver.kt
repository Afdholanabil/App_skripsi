package com.example.app_skripsi.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.app_skripsi.utils.NotificationSchedulerManager
import com.example.app_skripsi.workers.PredictionNotificationWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PredictionAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PredictionAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarm received at: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")

        // Execute prediction notification worker
        val workRequest = OneTimeWorkRequestBuilder<PredictionNotificationWorker>()
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)

        // Schedule next alarm for tomorrow
        val schedulerManager = NotificationSchedulerManager(context)
        schedulerManager.scheduleDailyPredictionNotificationWithAlarm(8, 0)
    }
}
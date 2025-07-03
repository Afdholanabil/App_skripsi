package com.healour.anxiety.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.healour.anxiety.data.local.RoutineSessionManager
import com.healour.anxiety.utils.NotificationSchedulerManager
import com.healour.anxiety.workers.RoutineFormReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RoutineFormAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "RoutineFormAlarmReceiver"
        const val EXTRA_REMINDER_NUMBER = "reminderNumber"
        const val EXTRA_USER_ID = "userId"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarm received at: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")

        val reminderNumber = intent.getIntExtra(EXTRA_REMINDER_NUMBER, 1)
        val userId = intent.getStringExtra(EXTRA_USER_ID)

        if (userId.isNullOrEmpty()) {
            Log.e(TAG, "User ID is null or empty")
            return
        }

        // Cek dulu apakah masih ada sesi aktif dan form belum diisi
        CoroutineScope(Dispatchers.IO).launch {
            val routineSessionManager = RoutineSessionManager(context)

            if (routineSessionManager.isSessionStillActive() && !routineSessionManager.hasCompletedFormToday()) {
                // Execute worker
                val workRequest = OneTimeWorkRequestBuilder<RoutineFormReminderWorker>()
                    .setInputData(workDataOf(
                        RoutineFormReminderWorker.KEY_REMINDER_NUMBER to reminderNumber,
                        "userId" to userId
                    ))
                    .build()

                WorkManager.getInstance(context).enqueue(workRequest)
            } else {
                Log.d(TAG, "Session not active or form already completed today, skipping notification")
            }

            // Jadwalkan ulang alarm berikutnya untuk besok
            val schedulerManager = NotificationSchedulerManager(context)
            schedulerManager.scheduleNextRoutineReminder(reminderNumber, userId)
        }
    }
}
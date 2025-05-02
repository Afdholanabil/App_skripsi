package com.example.app_skripsi.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.app_skripsi.data.local.RoutineSessionManager
import com.example.app_skripsi.utils.NotificationSchedulerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device boot completed, checking notification settings")
            CoroutineScope(Dispatchers.IO).launch {
                // Check if prediction notifications were enabled
                val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val notificationsEnabled = prefs.getBoolean(NotificationSchedulerManager.PREF_NOTIFICATIONS_ENABLED, false)

                if (notificationsEnabled) {
                    Log.d(TAG, "Rescheduling prediction notifications after boot")
                    val manager = NotificationSchedulerManager(context)
                    manager.scheduleDailyPredictionNotificationWithAlarm(8, 0)
                }
                // Check routine reminders
                val routineSessionManager = RoutineSessionManager(context)
                val userId = routineSessionManager.getUserId()

                if (userId != null && routineSessionManager.isSessionStillActive()) {
                    Log.d(TAG, "Rescheduling routine reminders for user: $userId")
                    val manager = NotificationSchedulerManager(context)
                    manager.scheduleRoutineFormRemindersWithAlarm(userId)
                }
            }


        }
    }
}
package com.example.app_skripsi.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.app_skripsi.data.local.RoutineSessionManager
import com.example.app_skripsi.data.local.SessionManager
import com.example.app_skripsi.utils.NotificationSchedulerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Broadcast receiver that listens for device boot completed events
 * and reschedules necessary notifications if there is an active routine session.
 */
class BootReceiver : BroadcastReceiver() {
    private lateinit var sessionManager: SessionManager
    private lateinit var notificationManager: NotificationSchedulerManager

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            Log.d(TAG, "Device boot completed, checking for active sessions")

            // Use coroutine to handle async operations
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Check if there's an active routine session
                    val routineSessionManager = RoutineSessionManager(context)
                    val notificationManager = NotificationSchedulerManager(context)
                    val userId = sessionManager.sessionUserId.first()

                    val isSessionActive = routineSessionManager.isSessionStillActive()

                    if (isSessionActive) {
                        Log.d(TAG, "Active routine session found after boot, rescheduling reminders")
                        notificationManager.scheduleRoutineFormRemindersWithAlarm(userId!!)
                    } else {
                        Log.d(TAG, "No active routine session found after boot")
                    }

                    // Check if prediction notifications should be rescheduled
                    // (You might want to store this preference in SharedPreferences)
                    // For now, we'll skip this unless the user has explicitly enabled it

                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling notifications after boot", e)
                }
            }
        }
    }
}
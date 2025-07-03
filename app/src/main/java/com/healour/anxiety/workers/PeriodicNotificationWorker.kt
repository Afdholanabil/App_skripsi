package com.healour.anxiety.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.healour.anxiety.MainActivity
import com.healour.anxiety.R

class PeriodicNotificationWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "PERIODIC_NOTIFICATION_CHANNEL"
        const val NOTIFICATION_ID = 2
    }

    override fun doWork(): Result {
        // Membuat notifikasi
        createNotificationChannel()

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Periodic Reminder")
            .setContentText("Ini adalah notifikasi periodik setiap 2 jam!")
            .setSmallIcon(R.drawable.ic_about)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Menampilkan notifikasi
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        // Mengembalikan status sukses
        return Result.success()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val name = "Periodic Notifications"
        val descriptionText = "Channel for periodic reminders every 2 hours"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            enableLights(true)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 200, 100, 200)
        }
        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}
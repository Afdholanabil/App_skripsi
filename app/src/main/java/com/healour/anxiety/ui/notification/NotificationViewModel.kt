package com.healour.anxiety.ui.notification

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.work.*
import com.healour.anxiety.workers.DailyNotificationWorker
import com.healour.anxiety.workers.PeriodicNotificationWorker
import java.util.concurrent.TimeUnit

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    fun scheduleDailyNotification() {
        try {
            // Membuat dan menjadwalkan pekerjaan untuk menampilkan notifikasi
            val workRequest = OneTimeWorkRequestBuilder<DailyNotificationWorker>()
                .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
                .build()

            // Menjadwalkan pekerjaan menggunakan WorkManager
            WorkManager.getInstance(getApplication()).enqueue(workRequest)
            Log.d("NotificationViewModel", "Notifikasi workmanager telah dibuat ${workRequest.tags}")

        } catch (e: Exception) {
            Log.e("NotificationViewModel", "Notifikasi workmanager gagal di buat", e)
        }
    }

    fun schedulePeriodicNotification() {
        try {
            // Membuat dan menjadwalkan pekerjaan untuk notifikasi periodik
            val workRequest = PeriodicWorkRequestBuilder<PeriodicNotificationWorker>(2, TimeUnit.HOURS)
                .setInitialDelay(0, TimeUnit.MILLISECONDS)  // Tidak ada delay awal, langsung mulai
                .build()

            // Menjadwalkan pekerjaan menggunakan WorkManager
            WorkManager.getInstance(getApplication()).enqueue(workRequest)
            Log.d("NotificationViewModel", "Notifikasi periodik telah dibuat ${workRequest.tags}")

        } catch (e: Exception) {
            Log.e("NotificationViewModel", "Notifikasi periodik gagal dibuat", e)
        }
    }

    private fun calculateInitialDelay(): Long {
        // Menghitung delay untuk menyesuaikan waktu tertentu (misal, 9 pagi setiap hari)
        val calendar = java.util.Calendar.getInstance()
        val targetTime = calendar.apply {
            set(java.util.Calendar.HOUR_OF_DAY, 5) // Jam 9 pagi
            set(java.util.Calendar.MINUTE, 23)
            set(java.util.Calendar.SECOND, 0)
        }.timeInMillis

        val currentTime = System.currentTimeMillis()
        return if (targetTime > currentTime) {
            targetTime - currentTime

        } else {
            // Jika waktu sudah lewat hari ini, atur untuk besok
            targetTime + TimeUnit.DAYS.toMillis(1) - currentTime
        }
    }
}
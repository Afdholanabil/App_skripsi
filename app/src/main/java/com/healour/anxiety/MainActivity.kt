package com.healour.anxiety

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.healour.anxiety.data.firebase.FirebaseService
import com.healour.anxiety.data.local.AppDatabase
import com.healour.anxiety.data.local.SessionManager
import com.healour.anxiety.data.repository.UserRepository
import com.healour.anxiety.ui.auth.login.LoginActivity
import com.healour.anxiety.ui.dashboard.DashboardActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build.VERSION
import android.util.Log

import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.BuildCompat
import androidx.media3.ui.BuildConfig
import androidx.startup.AppInitializer
import app.rive.runtime.kotlin.RiveInitializer
import app.rive.runtime.kotlin.core.Rive
import com.healour.anxiety.data.local.RoutineSessionManager
import com.healour.anxiety.utils.NotificationSchedulerManager
import com.healour.anxiety.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private var _binding : ActivityMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    // ✅ Initialize userRepository properly
    private val userRepository by lazy {
        val database = AppDatabase.getDatabase(this)
        UserRepository(FirebaseService(), database.userDao())
    }

    private lateinit var routineSessionManager: RoutineSessionManager
    private lateinit var notificationManager: NotificationSchedulerManager

    // Permission request launcher harus dideklarasikan sebagai variable
    private val permissionRequestLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Izin diberikan, lanjutkan aplikasi
                Log.d("MainActivity", "Izin notifikasi diberikan")
                startAppLogic()
            } else {
                // Izin ditolak, beri tahu pengguna tetapi tetap lanjutkan aplikasi
                Log.d("MainActivity", "Izin notifikasi ditolak")
                startAppLogic() // Tetap lanjutkan aplikasi meskipun izin ditolak
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        AppInitializer.getInstance(applicationContext)
            .initializeComponent(RiveInitializer::class.java)
        Rive.init(this)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        sessionManager = SessionManager(this)
        // Initialize session manager and notification scheduler
        routineSessionManager = RoutineSessionManager(this)
        notificationManager = NotificationSchedulerManager(this)

        // Periksa izin notifikasi hanya jika SDK >= 33 (Android 13)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkNotificationPermission()
        } else{
            startAppLogic()
        }

        // Check if we need to start routine form reminders
        checkAndScheduleRoutineReminders()
        // Handle deep links from notifications if needed
        handleNotificationIntent(intent.extras)

        binding.tvVersion.text = "HealOur: Anxiety ${com.healour.anxiety.BuildConfig.VERSION_NAME}"
    }

    // Fungsi untuk memeriksa izin
    private fun checkNotificationPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED) {
            // Izin sudah diberikan
            Log.d("MainActivity", "Izin notifikasi sudah diberikan")
            startAppLogic()
        } else {
            // Izin belum diberikan, minta izin
            Log.d("MainActivity", "Izin notifikasi belum diberikan, meminta izin")
            requestNotificationPermission()
        }
    }

    // Fungsi untuk meminta izin notifikasi
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionRequestLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startAppLogic()
        }
    }

    /**
     * Checks if routine session is active and schedules form reminders if needed
     */
    private fun checkAndScheduleRoutineReminders() {
        lifecycleScope.launch {
            try {
                // Dapatkan user ID saat ini
                val userId = sessionManager.sessionUserId.first()
                if (userId.isNullOrEmpty()) {
                    Log.d("MainActivity", "No user logged in, skipping routine reminders")
                    return@launch
                }
                // Restore session dari Firebase jika ada
                val isSessionRestored = routineSessionManager.restoreSessionFromFirebase(FirebaseService())

                if (isSessionRestored || routineSessionManager.isSessionStillActive()) {
                    Log.d("MainActivity", "Active routine session found for user: $userId, scheduling reminders")

                    // Ubah ke AlarmManager untuk routine reminders
                    notificationManager.scheduleRoutineFormRemindersWithAlarm(userId)
                } else {
                    Log.d("MainActivity", "No active routine session, cancelling reminders")
                    notificationManager.cancelRoutineFormAlarms()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error checking routine session", e)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Handle intent dari notifikasi
        handleNotificationIntent(intent.extras)
    }

    private fun handleNotificationIntent(extras: Bundle?) {
        if (extras == null) return

        val openForm = extras.getBoolean("OPEN_FORM", false)
        val detectionType = extras.getString("DETECTION_TYPE", null)

        if (openForm && detectionType == "ROUTINE") {
            Log.d("MainActivity", "Opening routine form from notification")

            // Buat intent untuk membuka DashboardActivity dengan flag
            val dashboardIntent = Intent(this, DashboardActivity::class.java).apply {
                putExtra("NAVIGATE_TO", "CHECK_ANXIETY")
                putExtra("DETECTION_TYPE", "ROUTINE")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            startActivity(dashboardIntent)
        }
    }


    private fun startAppLogic() {
        lifecycleScope.launch {
            delay(500)
            binding.tvNamaApp.alpha = 0f
            binding.tvNamaApp.animate().setDuration(3000).alpha(1f).withEndAction {
                lifecycleScope.launch {
                    delay(500)

                    sessionManager.sessionUserId.collect { userId ->
                        if (!userId.isNullOrEmpty()) {
                            Log.d("MainActivity", "📦 User Loaded from Session: $userId")
                            // Setelah login, check dan schedule reminders
                            checkAndScheduleRoutineReminders()
                            // Handle intent dari notifikasi jika ada
                            handleNotificationIntent(intent.extras)
                            startActivity(Intent(this@MainActivity, DashboardActivity::class.java))
                        } else {
                            Log.e("MainActivity", "❌ User ID Not Found, Redirecting to Login")
                            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        }
                        finish()
                    }
                }


            }
        }
    }

    companion object {

    }


}
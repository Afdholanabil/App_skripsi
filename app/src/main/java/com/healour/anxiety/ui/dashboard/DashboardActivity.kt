package com.healour.anxiety.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.healour.anxiety.R
import com.healour.anxiety.data.firebase.FirebaseService
import com.healour.anxiety.data.local.AppDatabase
import com.healour.anxiety.data.local.RoutineSessionManager
import com.healour.anxiety.data.local.SessionManager
import com.healour.anxiety.data.local.user.UserEntity
import com.healour.anxiety.data.repository.UserRepository
import com.healour.anxiety.databinding.ActivityDashboardBinding
import com.healour.anxiety.ui.auth.login.LoginActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {
    private var _binding : ActivityDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var pagerAdapter: DashboardPagerAdapter

    private val userRepository by lazy {
        val database = AppDatabase.getDatabase(application)
        UserRepository(FirebaseService(), database.userDao())
    }
    private val firebaseService by lazy {
        FirebaseService()
    }

    private val routineSessionManager by lazy {
        RoutineSessionManager(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bottomNav = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left , systemBars.top,
                systemBars.right , bottomNav.bottom)
            insets
        }
//        / Hide status bar & navigation bar
        hideSystemUI()
        // 🔹 Ambil User ID dari intent atau session
        lifecycleScope.launch {
            val userIdIntent = intent.getStringExtra("USER_ID") ?: ""
            SessionManager(this@DashboardActivity).sessionUserId.collect { userId ->
                Log.d("DashboardActivity","Try collecting userId from session : $userId")
                if (!userId.isNullOrEmpty()) {
                    // Load user data
                    loadUserData(userId)
                    syncRoutineSessionStatus(userId)
                } else {
                    // Redirect to login if no user session is found
                    startActivity(Intent(this@DashboardActivity, LoginActivity::class.java))
                    finish()
                }
            }
        }
        setupViewPager()
        setupBottomNavigation()
    }

    private suspend fun loadUserData(userId: String) {
        val localUser = userRepository.getUserFromLocal(userId)
        if (localUser != null) {
            viewModel.setUserData(localUser.nama, localUser.email)
            android.util.Log.d("DashboardActivity", "✅ User Data Loaded from SQLite: ${localUser.nama} - ${localUser.email}")
        } else {
            android.util.Log.e("DashboardActivity", "⚠️ User not found in SQLite, fetching from Firebase...")

            val firebaseUser = userRepository.getUserFromFirebase(userId)
            if (firebaseUser.isSuccess) {
                val user = firebaseUser.getOrNull()
                if (user != null) {
                    viewModel.setUserData(user.nama, user.email)
                    android.util.Log.d("DashboardActivity", "🔥 User Data Loaded from Firebase: ${user.nama} - ${user.email}")

                    // 🔹 Simpan ke SQLite untuk caching
                    userRepository.insertUser(
                        UserEntity(
                            userId = userId,
                            nama = user.nama,
                            email = user.email,
                            jenisKelamin = user.jenisKelamin,
                            umur = user.umur
                        )
                    )
                }
            }
        }
    }

    private suspend fun syncRoutineSessionStatus(userId: String) {
        try {
            // Set user ID di RoutineSessionManager
            routineSessionManager.setUserId(userId)

            // Cek status di DataStore
            val isLocalSessionActive = routineSessionManager.isSessionActive.first()

            // Cek status di Firebase
            val result = firebaseService.getActiveRoutineDetection(userId)
            val isFirebaseSessionActive = result.isSuccess && result.getOrNull() != null

            Log.d("DashboardActivity", "Sinkronisasi sesi rutin - Local: $isLocalSessionActive, Firebase: $isFirebaseSessionActive")

            if (isLocalSessionActive != isFirebaseSessionActive) {
                if (isFirebaseSessionActive) {
                    // Firebase aktif tapi lokal tidak, restore sesi
                    val success = routineSessionManager.restoreSessionFromFirebase(firebaseService)
                    Log.d("DashboardActivity", "Memulihkan sesi dari Firebase: $success")
                } else if (isLocalSessionActive) {
                    // Lokal aktif tapi Firebase tidak, akhiri sesi lokal
                    routineSessionManager.endSession()
                    Log.d("DashboardActivity", "Mengakhiri sesi lokal karena tidak ada sesi aktif di Firebase")
                }
            }

            // Verifikasi hari terakhir sesi saat ini
            if (isLocalSessionActive || isFirebaseSessionActive) {
                checkLastDayOfSession(userId)
            }
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error saat sinkronisasi sesi rutin: ${e.message}")
        }
    }

    private suspend fun checkLastDayOfSession(userId: String) {
        try {
            // Cek apakah hari ini adalah hari terakhir sesi
            val isLastDay = routineSessionManager.isTodayLastDayOfSession()
            val hasCompletedToday = routineSessionManager.hasCompletedFormToday()

            if (isLastDay && hasCompletedToday) {
                Log.d("DashboardActivity", "Hari terakhir sesi dan sudah diisi, akan mengakhiri sesi secara otomatis")

                // Akhiri sesi di Firebase
                val activeRoutineResult = firebaseService.getActiveRoutineDetection(userId)
                if (activeRoutineResult.isSuccess && activeRoutineResult.getOrNull() != null) {
                    val routineDoc = activeRoutineResult.getOrNull()!!
                    firebaseService.updateRoutineDetectionStatus(userId, routineDoc.first, false)

                    // Akhiri sesi lokal
                    routineSessionManager.endSession()

                    Log.d("DashboardActivity", "Sesi rutin berhasil diakhiri otomatis")
                }
            }
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error saat memeriksa hari terakhir sesi: ${e.message}")
        }
    }

    private fun setupViewPager() {
        pagerAdapter = DashboardPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.isUserInputEnabled = true  // Disable swipe navigation

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.bottomNavigation.menu.getItem(position).isChecked = true
            }
        })
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> binding.viewPager.currentItem = 0
                R.id.nav_check_anxiety -> binding.viewPager.currentItem = 1
                R.id.nav_profile -> binding.viewPager.currentItem = 2
            }
            true
        }
    }



    private fun hideSystemUI() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }
}
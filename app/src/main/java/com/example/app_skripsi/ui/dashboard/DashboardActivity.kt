package com.example.app_skripsi.ui.dashboard

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.app_skripsi.R
import com.example.app_skripsi.databinding.ActivityDashboardBinding

class DashboardActivity : AppCompatActivity() {
    private var _binding : ActivityDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var pagerAdapter: DashboardPagerAdapter
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
        // Ambil data user dari intent
        val name = intent.getStringExtra("USER_NAME") ?: "User"
        val email = intent.getStringExtra("USER_EMAIL") ?: "example@example.com"

        // Simpan ke ViewModel agar bisa diakses di fragment
        viewModel.setUserData(name, email)

        setupViewPager()
        setupBottomNavigation()
    }

    private fun setupViewPager() {
        pagerAdapter = DashboardPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.isUserInputEnabled = false  // Disable swipe navigation

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
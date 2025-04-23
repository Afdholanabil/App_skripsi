package com.example.app_skripsi.ui.profile.faq

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app_skripsi.R
import com.example.app_skripsi.data.model.FaqModel
import com.example.app_skripsi.databinding.ActivityAboutBinding
import com.example.app_skripsi.databinding.ActivityFaqBinding

class FaqActivity : AppCompatActivity() {
    private var _binding : ActivityFaqBinding? = null
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityFaqBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bottomNav = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left+v.paddingLeft , systemBars.top + v.paddingTop,
                systemBars.right + v.paddingRight , systemBars.bottom + v.paddingBottom)
            insets
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        hideSystemUI()
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val faqList = listOf(
            FaqModel("Apa itu GAD 7?", "GAD 7 ini adalah alat yang digunakan untuk mengukur tingkat kecemasan."),
            FaqModel("Apa itu kecemasan?", "Kecemasan adalah respon alami terhadap stres yang bisa bersifat sementara atau kronis."),
            FaqModel("Bagaimana cara mengatasi kecemasan?", "Mengelola kecemasan bisa dilakukan dengan teknik relaksasi, olahraga, atau terapi psikologis."),
            FaqModel("Apakah aplikasi ini bisa memberikan solusi?", "Aplikasi ini menyediakan rekomendasi berbasis GAD 7 dan saran untuk mengelola kecemasan."),
            FaqModel("Bagaimana cara menggunakan aplikasi ini?", "Cukup daftar, lakukan tes GAD 7, dan lihat hasil analisa kecemasan.")
        )

        binding.recyclerViewFaq.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewFaq.adapter = FaqAdapter(faqList)
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
}
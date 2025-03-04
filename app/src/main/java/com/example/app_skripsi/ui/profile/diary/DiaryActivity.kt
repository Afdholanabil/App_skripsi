package com.example.app_skripsi.ui.profile.diary

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.app_skripsi.R
import com.example.app_skripsi.data.firebase.FirebaseService
import com.example.app_skripsi.data.repository.DiaryRepository
import com.example.app_skripsi.databinding.ActivityDiaryBinding
import com.example.app_skripsi.ui.profile.diary.fragments.CreateDiaryFragment
import com.example.app_skripsi.ui.profile.diary.fragments.DiaryListFragment

class DiaryActivity : AppCompatActivity() {
    private var _binding : ActivityDiaryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DiaryViewModel by viewModels {
        DiaryViewModelFactory(DiaryRepository(FirebaseService()))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityDiaryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Set fragment pertama (List Diary)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, DiaryListFragment())
            .commit()
    }

    fun navigateToCreateDiary() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, CreateDiaryFragment())
            .addToBackStack(null) // Agar bisa kembali dengan tombol back
            .commit()
    }
    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
package com.example.app_skripsi.ui.profile.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.app_skripsi.data.repository.DiaryRepository

class DiaryViewModelFactory(private val diaryRepository: DiaryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DiaryViewModel::class.java) -> {
                DiaryViewModel(diaryRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
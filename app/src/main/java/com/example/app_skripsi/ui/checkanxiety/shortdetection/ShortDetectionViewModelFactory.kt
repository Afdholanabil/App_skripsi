package com.example.app_skripsi.ui.checkanxiety.shortdetection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.app_skripsi.data.repository.AnxietyRepository
import com.example.app_skripsi.ui.checkanxiety.FormAnxietyViewModel

class ShortDetectionViewModelFactory(private val anxietyRepository: AnxietyRepository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShortDetectionViewModel::class.java)) {
            return ShortDetectionViewModel(
                anxietyRepository,

            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

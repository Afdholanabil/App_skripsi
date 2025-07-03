package com.healour.anxiety.ui.checkanxiety.shortdetection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.healour.anxiety.data.repository.AnxietyRepository

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

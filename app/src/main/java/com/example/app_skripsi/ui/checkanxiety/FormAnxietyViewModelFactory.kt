package com.example.app_skripsi.ui.checkanxiety

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.app_skripsi.data.local.FormSessionManager
import com.example.app_skripsi.data.local.RoutineSessionManager
import com.example.app_skripsi.data.repository.AnxietyRepository

class FormAnxietyViewModelFactory(
    private val anxietyRepository: AnxietyRepository,
    private val formSessionManager: FormSessionManager,
    private val routineSessionManager: RoutineSessionManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FormAnxietyViewModel::class.java)) {
            return FormAnxietyViewModel(
                anxietyRepository,
                formSessionManager,
                routineSessionManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
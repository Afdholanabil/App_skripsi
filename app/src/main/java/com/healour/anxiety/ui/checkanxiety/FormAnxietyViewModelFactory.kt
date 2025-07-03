package com.healour.anxiety.ui.checkanxiety

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.healour.anxiety.data.local.FormSessionManager
import com.healour.anxiety.data.local.RoutineSessionManager
import com.healour.anxiety.data.repository.AnxietyRepository

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
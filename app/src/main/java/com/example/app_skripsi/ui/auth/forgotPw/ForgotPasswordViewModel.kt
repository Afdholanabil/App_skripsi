package com.example.app_skripsi.ui.auth.forgotPw

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_skripsi.data.repository.AuthRepository
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _resetResult = MutableLiveData<Result<Unit>>()
    val resetResult: LiveData<Result<Unit>> get() = _resetResult

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    fun sendPasswordReset(email: String) {
        _loading.value = true
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            _resetResult.value = result
            _loading.value = false
        }
    }
}
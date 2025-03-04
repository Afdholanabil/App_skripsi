package com.example.app_skripsi.ui.auth.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_skripsi.data.firebase.AuthService
import com.example.app_skripsi.data.model.UserModel
import com.example.app_skripsi.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _loginResult = MutableLiveData<Result<UserModel?>>()
    val loginResult: LiveData<Result<UserModel?>> get() = _loginResult

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    fun login(email: String, password: String) {
        _loading.value = true
        viewModelScope.launch {
            val loginResult = authRepository.loginUser(email, password)
            if (loginResult.isSuccess) {
                val userId = authRepository.getCurrentUserId()
                if (userId != null) {
                    val userData = authRepository.getUserData(userId)
                    _loginResult.value = userData
                } else {
                    _loginResult.value = Result.failure(Exception("Gagal mengambil userId"))
                }
            } else {
                _loginResult.value = Result.failure(loginResult.exceptionOrNull()!!)
            }
            _loading.value = false
        }
    }
}
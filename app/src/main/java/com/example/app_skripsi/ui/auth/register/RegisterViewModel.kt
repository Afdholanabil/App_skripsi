package com.example.app_skripsi.ui.auth.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_skripsi.data.firebase.AuthService
import com.example.app_skripsi.data.model.UserModel
import com.example.app_skripsi.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _registerResult = MutableLiveData<Result<Unit>>()
    val registerResult: LiveData<Result<Unit>> get() = _registerResult

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    fun register(email: String, password: String, user: UserModel) {
        _loading.value = true
        viewModelScope.launch {
            val result = authRepository.registerUser(email, password, user)
            _registerResult.value = result
            _loading.value = false
        }
    }
}
package com.example.app_skripsi.ui.auth.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_skripsi.data.firebase.AuthService
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class RegisterViewModel(private val authService: AuthService = AuthService()) : ViewModel() {
    private val _registerResult = MutableLiveData<Result<FirebaseUser?>>()
    val registerResult: LiveData<Result<FirebaseUser?>> get() = _registerResult

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    fun register(email: String, password: String) {
        _loading.value = true
        viewModelScope.launch {
            val result = authService.registerUser(email, password)
            _registerResult.value = result
            _loading.value = false
        }
    }
}
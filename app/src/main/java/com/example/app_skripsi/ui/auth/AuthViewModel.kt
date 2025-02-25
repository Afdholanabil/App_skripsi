package com.example.app_skripsi.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.app_skripsi.data.firebase.AuthService
import com.google.firebase.auth.FirebaseUser

class AuthViewModel(private val authService: AuthService = AuthService()):ViewModel() {
    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> get() = _user

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    fun checkCurrentUser() {
        _user.value = authService.getCurrentUser()
    }

    fun logout() {
        authService.logoutUser()
        _user.value = null
    }
}
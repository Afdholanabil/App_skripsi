package com.example.app_skripsi.ui.auth.login

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.app_skripsi.data.local.SessionManager
import com.example.app_skripsi.data.repository.AuthRepository
import com.example.app_skripsi.data.repository.UserRepository

@Suppress("UNCHECKED_CAST")
class LoginViewModelFactory(
    private val application: Application,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(application, authRepository, userRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

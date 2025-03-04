package com.example.app_skripsi.ui.auth


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.app_skripsi.data.repository.AuthRepository
import com.example.app_skripsi.ui.auth.forgotPw.ForgotPasswordViewModel
import com.example.app_skripsi.ui.auth.login.LoginViewModel
import com.example.app_skripsi.ui.auth.register.RegisterViewModel

class AuthViewModelFactory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(com.example.app_skripsi.ui.auth.login.LoginViewModel::class.java) -> {
                LoginViewModel(authRepository) as T
            }
            modelClass.isAssignableFrom(com.example.app_skripsi.ui.auth.register.RegisterViewModel::class.java) -> {
                RegisterViewModel(authRepository) as T
            }
            modelClass.isAssignableFrom(ForgotPasswordViewModel::class.java) -> {
                ForgotPasswordViewModel(authRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

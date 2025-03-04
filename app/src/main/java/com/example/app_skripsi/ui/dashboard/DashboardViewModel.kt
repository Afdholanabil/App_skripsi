package com.example.app_skripsi.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DashboardViewModel(): ViewModel()  {
    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> get() = _userName

    private val _userEmail = MutableLiveData<String>()
    val userEmail: LiveData<String> get() = _userEmail

    fun setUserData(name: String, email: String) {
        _userName.value = name
        _userEmail.value = email
    }

    fun setUserEmail(email: String) {
        _userEmail.value = email
    }

    // Fungsi untuk mengambil huruf pertama dari email sebagai avatar
    fun getAvatarInitial(): String {
        return _userName.value?.firstOrNull()?.lowercase() ?: "U"
    }

    fun getUserNameFromEmail(email: String): String {
        return email.substringBefore("@").replaceFirstChar { it.uppercaseChar() }
    }

    // Fungsi untuk mendapatkan salam sesuai jam saat ini
    fun getGreetingMessage(): String {
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (currentHour) {
            in 4..12 -> "Selamat Pagi"
            in 12..16 -> "Selamat Siang"
            in 16..20 -> "Selamat Sore"
            else -> "Selamat Malam"
        }
    }
}
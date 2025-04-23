package com.example.app_skripsi.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_skripsi.data.repository.UserRepository
import kotlinx.coroutines.launch

class DashboardViewModel(): ViewModel()  {
    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> get() = _userName

    private val _userEmail = MutableLiveData<String>()
    val userEmail: LiveData<String> get() = _userEmail

    fun loadUserFromSQLite(userRepository: UserRepository, userId: String) {
        viewModelScope.launch {
            val localUser = userRepository.getUserFromLocal(userId)
            if (localUser != null) {
                _userName.postValue(localUser.nama)
                _userEmail.postValue(localUser.email)
                android.util.Log.d("DashboardViewModel", "✅ Loaded user from SQLite: ${localUser.nama}")
            } else {
                android.util.Log.e("DashboardViewModel", "⚠️ No user found in SQLite")
            }
        }
    }

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
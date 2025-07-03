package com.healour.anxiety.ui.auth.login

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healour.anxiety.data.local.SessionManager
import com.healour.anxiety.data.local.user.UserEntity
import com.healour.anxiety.data.repository.AuthRepository
import com.healour.anxiety.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class LoginViewModel(
    application: Application,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val sessionManager = SessionManager(application)

    private val _loginResult = MutableLiveData<Result<UserEntity?>>()
    val loginResult: LiveData<Result<UserEntity?>> get() = _loginResult

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    fun login(email: String, password: String) {
        _loading.value = true
        viewModelScope.launch {
            val authResult = authRepository.loginUser(email, password)
            if (authResult.isSuccess) {
                val userId = authRepository.getCurrentUserId()
                if (userId != null) {
                    // ✅ Fetch user data from SQLite first, then Firebase if not available
                    val userEntity = userRepository.getUser(userId)
                    if (userEntity != null) {
                        _loginResult.value = Result.success(userEntity)

                        // 🔹 Generate session token from Firebase
                        val firebaseUser = FirebaseAuth.getInstance().currentUser
                        firebaseUser?.getIdToken(true)?.addOnSuccessListener { result ->
                            val token = result.token ?: "no_token"
                            val expiresAt = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(24)

                            viewModelScope.launch {
                                sessionManager.saveSession(token, userId, expiresAt)
                                android.util.Log.d("SessionManager", "Token Created: $token, Expiry: $expiresAt")
                            }
                        }
                    } else {
                        _loginResult.value = Result.failure(Exception("Failed to fetch user data"))
                    }
                } else {
                    _loginResult.value = Result.failure(Exception("Failed to get userId"))
                }
            } else {
                _loginResult.value = Result.failure(authResult.exceptionOrNull()!!)
            }
            _loading.value = false
        }
    }

//    fun login(email: String, password: String) {
//        _loading.value = true
//        viewModelScope.launch {
//            val loginResult = authRepository.loginUser(email, password)
//            if (loginResult.isSuccess) {
//                val userId = authRepository.getCurrentUserId()
//                if (userId != null) {
//                    val localUser = userRepository.getUserFromLocal(userId)
//                        _loginResult.value = Result.success(localUser)
////                    if (localUser != null) {
////                        android.util.Log.d("LoginFlow", "✅ Using SQLite Data for user: $userId")
////                    } else {
////                        val userData = userRepository.getUserFromFirebase(userId)
////                        if (userData.isSuccess) {
////                            val user = userData.getOrNull()
////                            if (user != null) {
////                                val userEntity = UserEntity(
////                                    userId = userId,
////                                    nama = user.nama,
////                                    email = user.email,
////                                    jenisKelamin = user.jenisKelamin,
////                                    umur = user.umur
////                                )
////                                userRepository.insertUser(userEntity) // ✅ Save to SQLite
////                                _loginResult.value = Result.success(userEntity)
////                                android.util.Log.d("LoginFlow", "🔥 Using Firebase Data and Cached: $userId")
////                            }
////                        } else {
////                            _loginResult.value = Result.failure(Exception("Failed to retrieve user data"))
////                            android.util.Log.e("LoginFlow", "❌ Failed to get data for user: $userId")
////                        }
////                    }
//
//                    // ✅ Store token session
//                    // Ambil token asli dari Firebase
//                    val firebaseUser = FirebaseAuth.getInstance().currentUser
//                    firebaseUser?.getIdToken(true)
//                        ?.addOnSuccessListener { result ->
//                            val token = result.token ?: "no_token"
//                            val expiresAt = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(24)
//
//                            // ✅ Jalankan di coroutine agar tidak error
//                            viewModelScope.launch {
//                                sessionManager.saveSession(token, userId, expiresAt)
//                                android.util.Log.d("SessionManager", "Token Created: $token, Expiry: $expiresAt")
//                            }
//                        }
//                } else {
//                    _loginResult.value = Result.failure(Exception("Failed to get userId"))
//                    android.util.Log.d("LoginFlow", "Failed to get userId")
//                }
//            } else {
//                _loginResult.value = Result.failure(loginResult.exceptionOrNull()!!)
//            }
//            _loading.value = false
//        }
//    }
}


// Simpan session setelah login berhasil
//                    val token = "dummy_token_${System.currentTimeMillis()}" // Bisa diganti dengan token asli
//                    val expiresAt = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(24) // Expired dalam 24 jam
//                    sessionManager.saveSession(token, userId, expiresAt)
//
//                    // 🔹 Cetak Log Token
//                    android.util.Log.d("SessionManager", "Token Created: $token, Expiry: $expiresAt")
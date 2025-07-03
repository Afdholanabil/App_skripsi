package com.healour.anxiety.data.repository

import com.healour.anxiety.data.firebase.FirebaseService
import com.healour.anxiety.data.model.UserModel

class AuthRepository(private val firebaseService: FirebaseService) {

    suspend fun registerUser(email: String, password: String, user: UserModel): Result<Unit> {
        return firebaseService.registerUser(email, password, user)
    }

    suspend fun loginUser(email: String, password: String): Result<Unit> {
        return firebaseService.loginUser(email, password)
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return firebaseService.sendPasswordResetEmail(email)
    }

    suspend fun logoutUser() {
        firebaseService.logoutUser()
    }


    suspend fun getCurrentUserId(): String? {
        return firebaseService.getCurrentUserId()
    }
    // 🔹 Ambil data user dari Firestore
    suspend fun getUserData(userId: String): Result<UserModel?> {
        return firebaseService.getDocumentById(
            collection = "users_students/data/userId",
            documentId = userId,
            clazz = UserModel::class.java
        )
    }
}
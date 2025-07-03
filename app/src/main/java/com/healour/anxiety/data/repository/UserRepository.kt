package com.healour.anxiety.data.repository

import com.healour.anxiety.data.firebase.FirebaseService
import com.healour.anxiety.data.local.user.UserDao
import com.healour.anxiety.data.local.user.UserEntity
import com.healour.anxiety.data.model.UserModel

class UserRepository(
    private val firebaseService: FirebaseService,
    private val userDao: UserDao
) {

    // ✅ Check SQLite First, then Fallback to Firebase
    suspend fun getUser(userId: String): UserEntity? {
        val localUser = userDao.getUserById(userId)  // ✅ Fetch from SQLite
        if (localUser != null) {
            android.util.Log.d("UserData", "📦 Retrieved from SQLite: $userId")
            return localUser
        }

        // 🔥 Fetch from Firebase if not available
        val firebaseUser = firebaseService.getUserData(userId)
        return if (firebaseUser.isSuccess) {
            firebaseUser.getOrNull()?.let { user ->
                val userEntity = UserEntity(
                    userId = userId,
                    nama = user.nama,
                    email = user.email,
                    jenisKelamin = user.jenisKelamin,
                    umur = user.umur
                )
                userDao.insertUser(userEntity) // ✅ Cache in SQLite
                android.util.Log.d("UserData", "🔥 Retrieved from Firebase and Cached: $userId")
                userEntity
            }
        } else {
            android.util.Log.e("UserData", "❌ Failed to get user from Firebase: $userId")
            null
        }
    }

    // ✅ Get from SQLite directly
    suspend fun getUserFromLocal(userId: String): UserEntity? {
        return userDao.getUserById(userId)
    }

    // ✅ Get from Firebase directly
    suspend fun getUserFromFirebase(userId: String): Result<UserModel?> {
        return firebaseService.getDocumentById(
            collection = "users_students/data/userId",
            documentId = userId,
            clazz = UserModel::class.java
        )
    }

    // ✅ Save to SQLite
    suspend fun insertUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    // ✅ Sync latest user data from SQLite to Firebase before logout
    suspend fun updateUserToFirebase(user: UserEntity): Result<Unit> {
        return firebaseService.addDocument(
            collection = "users_students/data/userId",
            documentId = user.userId,
            data = user
        )
    }

    // ✅ Clear SQLite when user logs out
    suspend fun clearLocalDatabase() {
        android.util.Log.e("SQLite", "🗑 Deleting all local user data...")
        userDao.deleteAllUsers()
        android.util.Log.e("SQLite", "✅ All local user data deleted")
    }
}
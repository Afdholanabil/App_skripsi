package com.example.app_skripsi.data.local.user

import androidx.room.*
import com.example.app_skripsi.data.local.user.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

//    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
//    fun getUser(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?  // ✅ Add this function


    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}
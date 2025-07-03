package com.healour.anxiety.data.local.user

import androidx.room.*

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
package com.example.app_skripsi.data.local.user

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.app_skripsi.data.model.UserModel

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val nama: String,
    val email: String,
    val jenisKelamin: String,
    val umur: Int
)

fun UserEntity.toUserModel(): UserModel {
    return UserModel(
        nama = this.nama,
        email = this.email,
        jenisKelamin = this.jenisKelamin,
        umur = this.umur
    )
}


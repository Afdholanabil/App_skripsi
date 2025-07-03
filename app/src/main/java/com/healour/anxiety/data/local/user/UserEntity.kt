package com.healour.anxiety.data.local.user

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.healour.anxiety.data.model.UserModel

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


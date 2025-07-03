package com.healour.anxiety.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserModel(
    var nama: String = "",
    var email: String = "",
    var jenisKelamin: String = "",
    var umur: Int = 0
) {
    // Constructor kosong dibutuhkan untuk Firebase Firestore
    constructor() : this("", "", "", 0)
}
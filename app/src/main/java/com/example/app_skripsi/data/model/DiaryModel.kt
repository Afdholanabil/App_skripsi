package com.example.app_skripsi.data.model
import com.google.firebase.firestore.IgnoreExtraProperties

import com.google.firebase.Timestamp

@IgnoreExtraProperties
data class DiaryModel(
    var isiDiary: Map<String, Any> = emptyMap(),
    var tanggal: Timestamp = Timestamp.now()
) {
    constructor() : this(emptyMap(), Timestamp.now())
}

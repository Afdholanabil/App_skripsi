package com.example.app_skripsi.data.model

import com.google.firebase.Timestamp

data class ShortDetectionModel(
    val emosi: String = "",
    val kegiatan: String = "",
    val tanggal: Timestamp = Timestamp.now(),
    val hari: String = "",
    val gad1: Int = 0,
    val gad2: Int = 0,
    val gad3: Int = 0,
    val gad4: Int = 0,
    val gad5: Int = 0,
    val gad6: Int = 0,
    val gad7: Int = 0,
    val total_skor: Int = 0,
    val severity: String = ""
)
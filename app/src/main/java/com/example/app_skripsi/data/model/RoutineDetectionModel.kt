package com.example.app_skripsi.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class RoutineDetectionModel(
    val aktif: Boolean = true,

    @get:PropertyName("deteksiHarian")
    @set:PropertyName("deteksiHarian")
    var deteksiHarian: Map<String, DailyDetectionData> = mapOf(),

    @get:PropertyName("hariSkorRendah")
    @set:PropertyName("hariSkorRendah")
    var hariSkorRendah: String = "",

    @get:PropertyName("hariSkorTinggi")
    @set:PropertyName("hariSkorTinggi")
    var hariSkorTinggi: String = "",

    @get:PropertyName("skorRendah")
    @set:PropertyName("skorRendah")
    var skorRendah: Int = 0,

    @get:PropertyName("skorTinggi")
    @set:PropertyName("skorTinggi")
    var skorTinggi: Int = 0,

    @get:PropertyName("periode")
    @set:PropertyName("periode")
    var periode: String = "",

    @get:PropertyName("tanggalMulai")
    @set:PropertyName("tanggalMulai")
    var tanggalMulai: Timestamp = Timestamp.now(),

    @get:PropertyName("tanggalSelesai")
    @set:PropertyName("tanggalSelesai")
    var tanggalSelesai: Timestamp = Timestamp.now()
)

data class DailyDetectionData(
    val emosi: String = "",
    val kegiatan: String = "",
    val gad1: Int = 0,
    val gad2: Int = 0,
    val gad3: Int = 0,
    val gad4: Int = 0,
    val gad5: Int = 0,
    val gad6: Int = 0,
    val gad7: Int = 0,
    val tanggal: Timestamp = Timestamp.now(),

    @get:PropertyName("total_skor")
    @set:PropertyName("total_skor")
    var totalSkor: Int = 0,

    val severity: String = ""
)
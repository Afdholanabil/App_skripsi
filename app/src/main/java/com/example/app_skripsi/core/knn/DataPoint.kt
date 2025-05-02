// 1. DataPoint.kt
package com.example.app_skripsi.core.knn

data class DataPoint(
    val emosi: Int,
    val aktivitas: Int,
    val hari: Int,
    val gadScore: Int,
    val label: String
)

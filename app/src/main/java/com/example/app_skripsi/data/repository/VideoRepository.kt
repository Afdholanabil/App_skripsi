package com.example.app_skripsi.data.repository

import com.example.app_skripsi.data.firebase.FirebaseService
import com.example.app_skripsi.data.model.VideoModel

class VideoRepository(private val firebaseService: FirebaseService) {

    suspend fun getAllVideos(): Result<List<VideoModel>> {
        return firebaseService.getVideos()
    }

    // Tetap sediakan metode ini untuk kompatibilitas jika diperlukan
    suspend fun getVideosByCategory(category: String): Result<List<VideoModel>> {
        // Abaikan kategori, ambil semua video
        return firebaseService.getVideos()
    }
}
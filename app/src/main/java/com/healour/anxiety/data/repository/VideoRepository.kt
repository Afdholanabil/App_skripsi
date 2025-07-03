package com.healour.anxiety.data.repository

import com.healour.anxiety.data.firebase.FirebaseService
import com.healour.anxiety.data.model.VideoModel

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
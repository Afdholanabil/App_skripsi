package com.healour.anxiety.ui.video

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healour.anxiety.data.model.VideoModel
import com.healour.anxiety.data.repository.VideoRepository
import kotlinx.coroutines.launch

class VideoViewModel(private val videoRepository: VideoRepository) : ViewModel() {

    private val _videos = MutableLiveData<List<VideoModel>>()
    val videos: LiveData<List<VideoModel>> = _videos

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Metode utama untuk memuat video
    fun loadAllVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val result = videoRepository.getAllVideos()

                result.fold(
                    onSuccess = { videoList ->
                        _videos.value = videoList
                        Log.d("VideoViewModel", "Loaded ${videoList.size} videos")
                    },
                    onFailure = { exception ->
                        _error.value = exception.message
                        Log.e("VideoViewModel", "Error loading videos: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message
                Log.e("VideoViewModel", "Exception: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Tetap sediakan metode ini untuk kompatibilitas
    fun loadVideosByCategory(category: String) {
        // Tetap menggunakan loadAllVideos() karena tidak ada kategori
        loadAllVideos()
    }

    fun clearError() {
        _error.value = null
    }
}
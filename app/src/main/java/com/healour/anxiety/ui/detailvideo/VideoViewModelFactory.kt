package com.healour.anxiety.ui.detailvideo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.healour.anxiety.data.repository.VideoRepository
import com.healour.anxiety.ui.video.VideoViewModel

class VideoViewModelFactory(private val videoRepository: VideoRepository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoViewModel::class.java)) {
            return VideoViewModel(videoRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
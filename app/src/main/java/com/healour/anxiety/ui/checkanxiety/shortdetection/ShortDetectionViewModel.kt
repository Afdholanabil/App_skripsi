package com.healour.anxiety.ui.checkanxiety.shortdetection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.healour.anxiety.data.model.ShortDetectionModel
import com.healour.anxiety.data.repository.AnxietyRepository

class ShortDetectionViewModel(private val anxietyRepository: AnxietyRepository) : ViewModel() {

    // MutableLiveData untuk menyimpan list deteksi singkat
    private val _shortDetectionList = MutableLiveData<List<ShortDetectionModel>>()
    val shortDetectionList: LiveData<List<ShortDetectionModel>> get() = _shortDetectionList

    // Fungsi untuk mengambil data deteksi singkat
    suspend fun fetchShortDetections() {
        anxietyRepository.getShortDetections().onSuccess { detections ->
            // Memperbarui LiveData dengan hasil deteksi singkat
            _shortDetectionList.value = detections
        }.onFailure {
            // Handle error jika terjadi kegagalan dalam pengambilan data
            _shortDetectionList.value = emptyList() // Menyediakan data kosong jika gagal
        }
    }
}

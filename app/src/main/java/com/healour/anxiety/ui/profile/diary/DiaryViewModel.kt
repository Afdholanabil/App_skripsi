package com.healour.anxiety.ui.profile.diary

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healour.anxiety.data.model.DiaryModel
import com.healour.anxiety.data.repository.DiaryRepository
import kotlinx.coroutines.launch

class DiaryViewModel(private val repository: DiaryRepository) : ViewModel() {
    private val _selectedEmotion = MutableLiveData<String>()
    val selectedEmotion: LiveData<String> get() = _selectedEmotion

    private val _diaries = MutableLiveData<List<DiaryModel>>()
    val diaries: LiveData<List<DiaryModel>> get() = _diaries

    private val _diarySaveResult = MutableLiveData<Result<Unit>>()
    val diarySaveResult: LiveData<Result<Unit>> get() = _diarySaveResult

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    fun setSelectedEmotion(emotion: String) {
        _selectedEmotion.value = emotion
    }

    fun fetchDiaries() {
        viewModelScope.launch {
            _loading.value = true
            val result = repository.getDiaries()
            if (result.isSuccess) {
                _diaries.value = result.getOrDefault(emptyList())
            }
            _loading.value = false
        }
    }

    fun addDiary(diary: DiaryModel) {
        viewModelScope.launch {
            _loading.value  = true
            val result = repository.addDiary(diary)
            if (result.isSuccess){
            _diarySaveResult.value = result
        }
            _loading.value = false
        }

    }
}
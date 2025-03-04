package com.example.app_skripsi.data.repository

import com.example.app_skripsi.data.firebase.FirebaseService
import com.example.app_skripsi.data.model.DiaryModel

class DiaryRepository(private val firebaseService: FirebaseService) {

    suspend fun addDiary(diary: DiaryModel): Result<Unit> {
        val userId = firebaseService.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
        return firebaseService.addDiary(userId, diary)
    }


    suspend fun getDiaries(): Result<List<DiaryModel>> {
        val userId = firebaseService.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
        return firebaseService.getDiaries(userId)
    }

}
package com.healour.anxiety.data.repository

import com.healour.anxiety.data.firebase.FirebaseService
import com.healour.anxiety.data.model.DiaryModel

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
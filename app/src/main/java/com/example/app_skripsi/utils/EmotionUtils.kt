package com.example.app_skripsi.utils

import com.example.app_skripsi.R

object EmotionUtils {
    fun getEmotionIcon(emotion: String): Int {
        return when (emotion.trim().lowercase()) {
            "senang" -> R.drawable.ic_happy
            "sedih" -> R.drawable.ic_sad
            "marah" -> R.drawable.ic_angry
            "normal" -> R.drawable.ic_normal
            "kecewa" -> R.drawable.ic_frustated
            else -> R.drawable.ic_normal // Default jika tidak dikenal
        }
    }
}
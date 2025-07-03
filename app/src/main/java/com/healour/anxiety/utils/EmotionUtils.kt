package com.healour.anxiety.utils

import com.healour.anxiety.R

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
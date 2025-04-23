package com.example.app_skripsi.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VideoModel(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: Int = 0,  // Resource ID untuk thumbnail default
    val sourceUrl: String = "",
    val hasCopyright: Boolean = false,
    val category: String = ""
) : Parcelable
package com.example.app_skripsi.data.model

data class FaqModel(val question: String,
                   val answer: String,
                   var isExpanded: Boolean = false)

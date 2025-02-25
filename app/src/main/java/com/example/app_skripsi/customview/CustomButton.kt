package com.example.app_skripsi.customview

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import com.example.app_skripsi.R


class CustomButton(context: Context, attrs: AttributeSet) : AppCompatButton(context, attrs) {
    init {
        setBackgroundResource(R.drawable.custom_button_background)
        setTextColor(resources.getColor(android.R.color.white, null))
        textSize = 16f
        isAllCaps = false
    }
}

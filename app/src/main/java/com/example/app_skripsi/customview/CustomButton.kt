package com.example.app_skripsi.customview

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.example.app_skripsi.R


class CustomButton(context: Context, attrs: AttributeSet) : AppCompatButton(context, attrs) {
    init {
        setBackgroundResource(R.drawable.selector_button_background)
        setTextColor(ContextCompat.getColor(context, R.color.white))
        textSize = 16f
        typeface = resources.getFont(R.font.inter_bold)
        isEnabled = false  // Default disabled
        isClickable = true // ⬅️ Aktifkan klik
        isFocusable = true // ⬅️ Aktifkan fokus
    }
}

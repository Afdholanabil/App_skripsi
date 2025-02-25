package com.example.app_skripsi.customview

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import com.example.app_skripsi.R
import com.google.android.material.textfield.TextInputEditText
import org.w3c.dom.Attr


class CustomTextField(context: Context, attr: AttributeSet) : TextInputEditText(context, attr) {
    init {
        background = resources.getDrawable(R.drawable.custom_textfield_background, null)
        setPadding(24, 24, 24, 24)
        textSize = 14f
        setTextColor(resources.getColor(R.color.gray800, null))
    }
}

package com.healour.anxiety.customview

import android.content.Context
import android.util.AttributeSet
import com.healour.anxiety.R
import com.google.android.material.textfield.TextInputEditText


class CustomTextField(context: Context, attr: AttributeSet) : TextInputEditText(context, attr) {

    init {
        setPadding(40, 40, 40, 40)  // ✅ Padding untuk kenyamanan input
        setTextColor(resources.getColor(R.color.gray800, null))
        setHintTextColor(resources.getColor(R.color.gray500, null))
        textSize = 16f
        typeface = resources.getFont(R.font.inter_regular)
        setBackgroundResource(R.drawable.textfield_outline_background)  // ✅ Background outline
    }
}

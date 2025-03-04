package com.example.app_skripsi.utils

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.example.app_skripsi.R

 object ToastUtils {
    enum class Position { TOP, BOTTOM }

    fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT, position: Position = Position.BOTTOM) {
        val inflater = LayoutInflater.from(context)
        val layout: View = inflater.inflate(R.layout.custom_toast, null)

        val text: TextView = layout.findViewById(R.id.toast_text)
        text.text = message

        val toast = Toast(context)
        toast.duration = duration
        toast.view = layout
        toast.setGravity(
            if (position == Position.TOP) Gravity.TOP or Gravity.FILL_HORIZONTAL else Gravity.BOTTOM or Gravity.FILL_HORIZONTAL,
            0,
            100
        )
        toast.show()
    }
}
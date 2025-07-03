package com.healour.anxiety.ui.checkanxiety

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.healour.anxiety.ui.checkanxiety.formanxiety.PermissionFragment
import com.healour.anxiety.ui.checkanxiety.formanxiety.EmotionFragment
import com.healour.anxiety.ui.checkanxiety.formanxiety.ActivityFragment
import com.healour.anxiety.ui.checkanxiety.formanxiety.GadQuestionFragment

class FormAnxietyAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    // Mengembalikan jumlah fragment yang ada
    override fun getItemCount(): Int = 10

    // Mengembalikan fragment yang sesuai dengan posisi yang dipilih
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PermissionFragment() // Fragment pertama untuk izin
            1 -> EmotionFragment() // Fragment kedua untuk pemilihan emosi
            2 -> ActivityFragment() // Fragment ketiga untuk pemilihan aktivitas
            else -> {
                // Pertanyaan GAD-7 (position 3-9)
                val gadPosition = position - 3
                Log.d("FormAnxietyAdapter", "Creating GAD Question Fragment position=$position, gadPosition=$gadPosition")
                val isLastQuestion = gadPosition == 6 // Pertanyaan terakhir
                GadQuestionFragment.newInstance(
                    questionNumber = gadPosition,
                    questionText = getGadQuestion(gadPosition),
                    isLast = isLastQuestion
                )
            }
        }
    }
    private fun getGadQuestion(position: Int): String {
        val questions = listOf(
            "Merasa gugup, cemas, atau tegang",
            "Tidak mampu menghentikan atau mengendalikan rasa khawatir",
            "Terlalu mengkhawatirkan berbagai hal",
            "Sulit untuk rileks",
            "Sangat gelisah sehingga sulit untuk duduk diam",
            "Menjadi mudah tersinggung atau mudah marah",
            "Merasa takut, seolah-olah ada sesuatu yang buruk mungkin terjadi"
        )
        return questions[position]
    }
}
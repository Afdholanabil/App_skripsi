package com.example.app_skripsi.ui.checkanxiety.formanxiety

import android.os.Bundle
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.app_skripsi.R
import com.example.app_skripsi.databinding.FragmentEmotionBinding
import com.example.app_skripsi.databinding.FragmentHasilAnxietyBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HasilAnxietyFragment : Fragment() {
    private var _binding : FragmentHasilAnxietyBinding? = null
    private val binding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHasilAnxietyBinding.inflate(inflater,container,false)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Retrieve data from intent
        val totalScore = arguments?.getInt("TOTAL_SCORE", 0) ?: 0
        val emotion = arguments?.getString("EMOTION") ?: "Tidak Diketahui"
        val activity = arguments?.getString("ACTIVITY") ?: "Tidak Diketahui"

        // Set current date
        val currentDate = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID")).format(Date())
        binding.tvDate.text = "Hari: $currentDate"

        // Set emotion and activity
        binding.tvEmotion.text = "Emosi: $emotion"
        binding.tvActivity.text = "Kegiatan: $activity"

        // Set total score and anxiety level
        binding.tvTotalSkor.text = "Total Skor GAD-7: $totalScore"
        setAnxietyLevelAndAdvice(totalScore)

        // Back button
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // Optional: Set individual GAD scores if available
        setGadScores()
    }

    private fun setAnxietyLevelAndAdvice(totalScore: Int) {
        val (level, color, advice) = when {
            totalScore in 0..4 -> Triple("Minimal", "#4CAF50",
                "Tingkat kecemasan Anda minimal. Terus pertahankan kesehatan mental dan gaya hidup sehat.")
            totalScore in 5..9 -> Triple("Ringan", "#7986CB",
                "Anda mungkin bisa mengelola ini secara mandiri dengan teknik manajemen stres, latihan relaksasi, dan perubahan gaya hidup seperti tidur cukup dan makan makanan sehat.")
            totalScore in 10..14 -> Triple("Sedang", "#FF9800",
                "Tingkat kecemasan Anda moderat. Pertimbangkan untuk mencari dukungan dari teman, keluarga, atau profesional kesehatan mental.")
            else -> Triple("Berat", "#F44336",
                "Tingkat kecemasan Anda tinggi. Sangat disarankan untuk segera berkonsultasi dengan profesional kesehatan mental.")
        }

        binding.btnAnxietyManagement.text = "Kecemasan $level"
        binding.btnAnxietyManagement.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(color))
        binding.tvAdvice.text = advice
    }

    private fun setGadScores() {
        // Ambil semua jawaban GAD dari arguments
        val gadScores = (1..7).map { index ->
            val score = arguments?.getInt("GAD$index", -1) ?: -1
            "GAD-$index: ${getGadScoreDescription(score)}"
        }

        // Clear existing views
        binding.layoutGADScores.removeAllViews()

        // Add GAD scores dynamically
        gadScores.forEach { score ->
            val textView = TextView(requireContext()).apply {
                text = score
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.gray800))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8, 0, 8) // Tambahkan margin antar teks
                }
            }
            binding.layoutGADScores.addView(textView)
        }
    }

    private fun getGadScoreDescription(score: Int): String {
        return when (score) {
            0 -> "Tidak Pernah"
            1 -> "Beberapa Hari"
            2 -> "Lebih dari Setengah Hari"
            3 -> "Hampir Setiap Hari"
            else -> "Tidak Diketahui"
        }
    }

    companion object {
        fun newInstance(
            totalScore: Int,
            emotion: String,
            activity: String,
            gadScores: List<Int>? = null
        ): HasilAnxietyFragment {
            val fragment = HasilAnxietyFragment()
            val args = Bundle().apply {
                putInt("TOTAL_SCORE", totalScore)
                putString("EMOTION", emotion)
                putString("ACTIVITY", activity)

                // Optionally add individual GAD scores
                gadScores?.forEachIndexed { index, score ->
                    putInt("GAD${index + 1}", score)
                }
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
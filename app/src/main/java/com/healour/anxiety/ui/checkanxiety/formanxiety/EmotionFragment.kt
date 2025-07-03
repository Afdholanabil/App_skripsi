package com.healour.anxiety.ui.checkanxiety.formanxiety

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.healour.anxiety.R
import com.healour.anxiety.data.local.FormSessionManager
import com.healour.anxiety.databinding.FragmentEmotionBinding
import com.healour.anxiety.ui.checkanxiety.FormAnxietyActivity
import com.healour.anxiety.ui.checkanxiety.FormAnxietyViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class EmotionFragment : Fragment() {
    private var _binding : FragmentEmotionBinding? = null
    private val binding get() = _binding!!

    private lateinit var formSessionManager: FormSessionManager
    private lateinit var emotionMap: Map<ImageView, String>

    // Ubah dari by viewModels() menjadi lateinit var
    private lateinit var viewModel: FormAnxietyViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEmotionBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        formSessionManager = FormSessionManager(requireContext())

        viewModel = (requireActivity() as FormAnxietyActivity).viewModel

        // Cek apakah sudah ada emosi yang dipilih sebelumnya
        MainScope().launch {
            val savedEmotion = formSessionManager.emotion.first()
            if (savedEmotion.isNotEmpty()) {
                viewModel.setSelectedEmotion(savedEmotion)
            }
        }

        emotionMap = mapOf(
            binding.ivHappy to "Senang",
            binding.ivSad to "Sedih",
            binding.ivNormal to "Normal",
            binding.ivAngry to "Marah",
            binding.ivFrustated to "Kecewa"
        )

        // Dalam EmotionFragment
        binding.btnLanjutkan.setOnClickListener {
            // Menyimpan pilihan emosi ke DataStore
            val selectedEmotion = viewModel.selectedEmotion.value ?: return@setOnClickListener
            MainScope().launch {
                formSessionManager.saveEmotion(selectedEmotion) // Menyimpan pilihan emosi

                // Pindah ke halaman berikutnya dengan ViewPager2
                val viewPager2 = activity?.findViewById<ViewPager2>(R.id.fragment_container)
                viewPager2?.currentItem = 2 // Pindah ke fragment ActivityFragment (index 2)

                // Update indikator ke langkah berikutnya
                (activity as FormAnxietyActivity).updateProgressIndicator(3) // Update indikator ke langkah berikutnya
            }
        }

        setupEmotionSelection()

    }

    private fun setupEmotionSelection() {
        // Set click listeners for all emotion images
        emotionMap.keys.forEach { imageView ->
            imageView.setOnClickListener {
                val selectedEmotion = emotionMap[imageView] ?: return@setOnClickListener
                viewModel.setSelectedEmotion(selectedEmotion)

                // Set the text visibility based on the selection
                getEmotionTextView(selectedEmotion)
            }
        }

        // Observe the selected emotion and update UI accordingly
        viewModel.selectedEmotion.observe(viewLifecycleOwner) { selectedEmotion ->
            emotionMap.forEach { (imageView, emotion) ->
                val color = if (emotion == selectedEmotion) R.color.bluePrimary else R.color.gray400
                imageView.setColorFilter(ContextCompat.getColor(requireContext(), color))
                // Show/hide the text based on selected emotion
                if (emotion == selectedEmotion) {
                    // Make TextView visible for selected emotion
                    getEmotionTextView(emotion)?.visibility = View.VISIBLE
                } else {
                    // Hide TextView for unselected emotions
                    getEmotionTextView(emotion)?.visibility = View.GONE
                }
            }
        }
    }

    // Function to get TextView based on selected emotion
    private fun getEmotionTextView(emotion: String): TextView? {
        return when (emotion) {
            "Senang" -> binding.tvHappy
            "Sedih" -> binding.tvSad
            "Normal" -> binding.tvNormal
            "Marah" -> binding.tvAngry
            "Kecewa" -> binding.tvFrustated
            else -> null
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            EmotionFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }
}
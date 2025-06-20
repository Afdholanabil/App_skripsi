package com.example.app_skripsi.ui.profile.diary.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import com.example.app_skripsi.R
import com.example.app_skripsi.data.firebase.FirebaseService
import com.example.app_skripsi.data.model.DiaryModel
import com.example.app_skripsi.data.repository.DiaryRepository
import com.example.app_skripsi.databinding.FragmentCreateDiaryBinding
import com.example.app_skripsi.ui.profile.diary.DiaryViewModel
import com.example.app_skripsi.ui.profile.diary.DiaryViewModelFactory
import com.example.app_skripsi.utils.ToastUtils
import androidx.fragment.app.viewModels



class CreateDiaryFragment : Fragment() {
    private var _binding: FragmentCreateDiaryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DiaryViewModel by viewModels {
        DiaryViewModelFactory(DiaryRepository(FirebaseService()))
    }

    private lateinit var emotionMap: Map<ImageView, String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateDiaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener {
            requireActivity().finish()
        }
        emotionMap = mapOf(
            binding.ivHappy to "Senang",
            binding.ivSad to "Sedih",
            binding.ivNormal to "Normal",
            binding.ivAngry to "Marah",
            binding.ivFrustated to "Kecewa"
        )

        val actitityToday = resources.getStringArray(R.array.activity_today)
        val aTodayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, actitityToday)
        binding.actvActivityType.setAdapter(aTodayAdapter)

        setupEmotionSelection()
        setupSaveButton()
        observeDiarySaveResult()
        setupTextWatchers()
    }

    private fun setupEmotionSelection() {
        emotionMap.keys.forEach { imageView ->
            imageView.setOnClickListener {
                val selectedEmotion = emotionMap[imageView] ?: return@setOnClickListener
                viewModel.setSelectedEmotion(selectedEmotion)
            }
        }

        viewModel.selectedEmotion.observe(viewLifecycleOwner) { selectedEmotion ->
            emotionMap.forEach { (imageView, emotion) ->
                val color = if (emotion == selectedEmotion) R.color.bluePrimary else R.color.gray400
                imageView.setColorFilter(ContextCompat.getColor(requireContext(), color))

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

    private fun setupSaveButton() {
        binding.btnSimpanDiary.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val content = binding.etContent.text.toString().trim()
            val activity = binding.actvActivityType.text.toString().trim()
            val emotion = viewModel.selectedEmotion.value ?: "Normal"

            if (title.isEmpty() || content.isEmpty() || activity.isEmpty()) {
                Toast.makeText(requireContext(), "Semua field harus diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Buat data dengan struktur yang benar
            val diaryData = DiaryModel(
                isiDiary = mapOf(
                    "judul" to title,
                    "isi" to content,
                    "kegiatan" to activity,
                    "emosi" to emotion
                ),
                tanggal = com.google.firebase.Timestamp.now()
            )

            viewModel.addDiary(diaryData)
        }
    }


    private fun observeDiarySaveResult() {
        viewModel.diarySaveResult.observe(viewLifecycleOwner) { result ->
            if (result.isSuccess) {
                ToastUtils.showToast(requireContext(),"Diary berhasil disimpan!", Toast.LENGTH_SHORT, position = ToastUtils.Position.TOP)

                requireActivity().onBackPressed() // Kembali ke list diary
            } else {
                ToastUtils.showToast(requireContext(),"Gagal menyimpan diary: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT, position = ToastUtils.Position.TOP)

            }
        }
    }

    private fun setButtonState() {
        val isAllFieldFilled = binding.actvActivityType.text.toString().isNotEmpty() && binding.etTitle.text.toString().isNotEmpty()
                &&binding.etContent.text.toString().isNotEmpty()

        binding.btnSimpanDiary.isEnabled = isAllFieldFilled
    }

    private fun setupTextWatchers() {
        binding.actvActivityType.addTextChangedListener { setButtonState() }
        binding.etTitle.addTextChangedListener { setButtonState() }
        binding.etContent.addTextChangedListener { setButtonState() }
    }

    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            CreateDiaryFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
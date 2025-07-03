package com.healour.anxiety.ui.checkanxiety.formanxiety

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.healour.anxiety.R
import com.healour.anxiety.data.firebase.FirebaseService
import com.healour.anxiety.data.local.FormSessionManager
import com.healour.anxiety.data.local.RoutineSessionManager
import com.healour.anxiety.data.repository.AnxietyRepository
import com.healour.anxiety.databinding.FragmentGadQuestionBinding
import com.healour.anxiety.ui.checkanxiety.FormAnxietyActivity
import com.healour.anxiety.ui.checkanxiety.FormAnxietyViewModel
import com.healour.anxiety.ui.checkanxiety.HasilAnxietyShortActivity
import kotlinx.coroutines.launch


class GadQuestionFragment : Fragment() {
    private var _binding: FragmentGadQuestionBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: FormAnxietyViewModel
    private lateinit var formSessionManager: FormSessionManager

    private var questionNumber: Int = 0
    private var questionText: String = ""  // Menambahkan deklarasi variabel questionText
    private var isLastQuestion: Boolean = false
    private var selectedAnswer: Int = -1
    private var hasNavigated = false

    // Untuk menyimpan jawaban GAD
    private var gadAnswers = mutableListOf<Int?>(null, null, null, null, null, null, null)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            questionNumber = it.getInt(ARG_QUESTION_NUMBER)
            questionText = it.getString(ARG_QUESTION_TEXT, "")
            isLastQuestion = it.getBoolean(ARG_IS_LAST, false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentGadQuestionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        formSessionManager = FormSessionManager(requireContext())
        viewModel = (activity as FormAnxietyActivity).viewModel

        Log.d(TAG, "onViewCreated: Question #$questionNumber, isLast=$isLastQuestion, text='$questionText'")

        // Inisialisasi array jawaban
        gadAnswers = mutableListOf<Int?>(null, null, null, null, null, null, null)

        // Setup pertanyaan
        binding.tvQuestionNumber.text = "Pertanyaan ${questionNumber + 1}"
        binding.tvQuestion.text = questionText  // Menggunakan tvQuestion sesuai dengan binding

        // Atur teks tombol berdasarkan apakah ini pertanyaan terakhir
        binding.btnLanjutkan.text = if (isLastQuestion) "Selesai" else "Selanjutnya"

        // Load jawaban yang sudah disimpan (jika ada)
        loadSavedAnswer()

        // Setup radio button
        setupRadioButtons()

        // Setup tombol selanjutnya
        binding.btnLanjutkan.setOnClickListener {
            Log.d(TAG, "Button Lanjutkan diklik untuk pertanyaan $questionNumber")
            handleNextButtonClick()
        }
    }

    private fun loadSavedAnswer() {
        lifecycleScope.launch {
            try {
                val savedAnswer = formSessionManager.getGadAnswer(questionNumber)
                Log.d(TAG, "Memuat jawaban tersimpan untuk pertanyaan $questionNumber: $savedAnswer")

                if (savedAnswer > 0) {
                    // Jika ada jawaban tersimpan, set radio button
                    when (savedAnswer) {
                        1 -> binding.rbOption1.isChecked = true
                        2 -> binding.rbOption2.isChecked = true
                        3 -> binding.rbOption3.isChecked = true
                        4 -> binding.rbOption4.isChecked = true
                    }

                    // Update array jawaban (konversi 1-4 menjadi 0-3)
                    gadAnswers[questionNumber] = savedAnswer - 1

                    // Enable tombol lanjutkan
                    binding.btnLanjutkan.isEnabled = true
                } else {
                    // Tidak ada jawaban tersimpan
                    binding.btnLanjutkan.isEnabled = false
                    Log.d(TAG, "Tidak ada jawaban tersimpan untuk pertanyaan $questionNumber")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading saved answer: ${e.message}")
            }
        }
    }

    private fun setupRadioButtons() {
        binding.rbOption1.setOnClickListener { updateAnswer(0) } // Not at all
        binding.rbOption2.setOnClickListener { updateAnswer(1) } // Several days
        binding.rbOption3.setOnClickListener { updateAnswer(2) } // More than half the days
        binding.rbOption4.setOnClickListener { updateAnswer(3) } // Nearly every day
    }

    private fun updateAnswer(answerValue: Int) {
        // Simpan di array lokal (0-3)
        gadAnswers[questionNumber] = answerValue
        Log.d(TAG, "Mengupdate jawaban pertanyaan $questionNumber ke nilai $answerValue")

        // Aktifkan tombol lanjutkan
        binding.btnLanjutkan.isEnabled = true

        // Simpan jawaban ke DataStore (1-4)
        lifecycleScope.launch {
            val valueToSave = answerValue + 1
            formSessionManager.saveGadAnswer(questionNumber, valueToSave)
            Log.d(TAG, "Menyimpan ke DataStore: pertanyaan $questionNumber = $valueToSave")
        }
    }

    private fun handleNextButtonClick() {
        // Verifikasi bahwa pertanyaan ini telah dijawab
        if (gadAnswers[questionNumber] == null) {
            Toast.makeText(context, "Silakan pilih salah satu jawaban", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            // Simpan jawaban ke DataStore
            val answerToSave = (gadAnswers[questionNumber] ?: 0) + 1 // Konversi 0-3 menjadi 1-4
            formSessionManager.saveGadAnswer(questionNumber, answerToSave)
            Log.d(TAG, "Menyimpan jawaban untuk pertanyaan $questionNumber: $answerToSave")

            if (isLastQuestion) {
                // Double check: jika ini deteksi rutin, cek apakah sudah mengisi hari ini
                val detectionType = formSessionManager.getDetectionType()

                // Periksa apakah ada session rutin yang aktif dan belum diisi hari ini
                var canProceed = true
                if (detectionType == "ROUTINE") {
                    try {
                        val routineSessionManager = RoutineSessionManager(requireContext())
                        val hasCompletedToday = routineSessionManager.hasCompletedFormToday()
                        Log.d(TAG, "Checking routine completion status: hasCompletedToday=$hasCompletedToday")

                        if (hasCompletedToday) {
                            Log.w(TAG, "Final check: User has already completed form today")
                            Toast.makeText(
                                requireContext(),
                                "Anda sudah mengisi form deteksi kecemasan hari ini",
                                Toast.LENGTH_LONG
                            ).show()
                            requireActivity().finish()
                            canProceed = false
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error checking form completion status", e)
                        // Tetap lanjutkan jika terjadi error pada pengecekan
                    }
                }

                // Hanya lanjutkan jika belum mengisi hari ini atau bukan deteksi rutin
                if (canProceed) {
                    // Ambil jawaban langsung dari DataStore, bukan dari array lokal
                    var totalScore = 0
                    val allAnswers = ArrayList<Int>()
                    var hasAllAnswers = true

                    // Log untuk debug
                    Log.d(TAG, "Memeriksa jawaban GAD dari DataStore:")

                    for (i in 0..6) {
                        val answer = formSessionManager.getGadAnswer(i)
                        Log.d(TAG, "Jawaban GAD $i dari DataStore: $answer")

                        if (answer <= 0) { // Nilai -1 berarti jawaban tidak ada
                            hasAllAnswers = false
                            Log.e(TAG, "Jawaban untuk pertanyaan $i tidak ditemukan")
                            break
                        } else {
                            // Simpan dengan konversi dari skala DataStore (1-4) ke skala aplikasi (0-3)
                            val adjustedAnswer = answer - 1
                            totalScore += adjustedAnswer
                            allAnswers.add(adjustedAnswer)
                        }
                    }

                    if (!hasAllAnswers) {
                        // Coba perbaiki jawaban yang hilang jika mungkin
                        Log.d(TAG, "Beberapa jawaban tidak lengkap, mencoba inisialisasi ulang dari pertanyaan awal")
                        if (binding.tvErrorMessage != null) {
                            binding.tvErrorMessage.visibility = View.VISIBLE
                            binding.tvErrorMessage.text = "Mohon jawab semua pertanyaan sebelumnya"
                        }

                        // Tampilkan log sebagai bantuan debugging
                        for (i in 0..6) {
                            val answer = formSessionManager.getGadAnswer(i)
                            Log.d(TAG, "Pengecekan ulang GAD $i: $answer")
                        }
                    } else {
                        // Simpan total skor
                        formSessionManager.saveGadTotalScore(totalScore)
                        Log.d(TAG, "Menyimpan total skor: $totalScore")

                        // Ambil data emosi dan aktivitas
                        val emotion = formSessionManager.getEmotion()
                        val activity = formSessionManager.getActivity()

                        Log.d(TAG, "Data lengkap untuk hasil: Emosi=$emotion, Aktivitas=$activity, Total Skor=$totalScore")

                        // PERBAIKAN: Simpan data ke repository dan TUNGGU selesai
                        val detectionType = formSessionManager.getDetectionType()

                        if (detectionType == "QUICK") {
                            // Untuk deteksi singkat, tunggu save selesai
                            Log.d(TAG, "Saving quick detection to Firestore...")

                            // Disable button sementara
                            binding.btnLanjutkan.isEnabled = false
                            binding.btnLanjutkan.text = "Menyimpan..."

                            // Panggil ViewModel dan tunggu hasilnya
                            val firebaseService = FirebaseService()
                            val anxietyRepository = AnxietyRepository(firebaseService)

                            // Simpan langsung ke repository
                            val saveResult = anxietyRepository.addShortDetection(emotion, activity, allAnswers, totalScore)

                            if (saveResult.isSuccess) {
                                Log.d(TAG, "Quick detection saved successfully")

                                // Navigasi ke hasil setelah save berhasil
                                val intent = Intent(requireActivity(), HasilAnxietyShortActivity::class.java)
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                intent.putExtra("TOTAL_SCORE", totalScore)
                                intent.putExtra("EMOTION", emotion)
                                intent.putExtra("ACTIVITY", activity)
                                intent.putIntegerArrayListExtra("GAD_ANSWERS", ArrayList(allAnswers))
                                startActivity(intent)
                                requireActivity().finish()

                            } else {
                                Log.e(TAG, "Failed to save quick detection: ${saveResult.exceptionOrNull()?.message}")
                                Toast.makeText(requireContext(), "Gagal menyimpan data", Toast.LENGTH_LONG).show()

                                // Enable button kembali
                                binding.btnLanjutkan.isEnabled = true
                                binding.btnLanjutkan.text = "Selesai"
                            }

                        } else {
                            // Untuk routine, gunakan ViewModel seperti biasa
                            // Tambahkan backup marking untuk routine detection
                            if (detectionType == "ROUTINE") {
                                try {
                                    val routineSessionManager = RoutineSessionManager(requireContext())
                                    routineSessionManager.saveFormCompletionForToday()
                                    Log.d(TAG, "GadQuestion marking routine completion for today (backup)")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error saving completion status", e)
                                }
                            }

                            // Navigasi ke hasil langsung untuk routine
                            val intent = Intent(requireActivity(), HasilAnxietyShortActivity::class.java)
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            intent.putExtra("TOTAL_SCORE", totalScore)
                            intent.putExtra("EMOTION", emotion)
                            intent.putExtra("ACTIVITY", activity)
                            intent.putIntegerArrayListExtra("GAD_ANSWERS", ArrayList(allAnswers))
                            startActivity(intent)
                            requireActivity().finish()
                        }
                    }
                }
            } else {
                // Untuk pertanyaan non-terakhir, navigasi ke pertanyaan berikutnya
                try {
                    // Cara 1: Gunakan fungsi navigasi di activity
                    val activity = requireActivity() as FormAnxietyActivity
                    activity.navigateToGadQuestion(questionNumber + 1)
                    Log.d(TAG, "Navigasi ke pertanyaan ${questionNumber + 1}")
                } catch (e: Exception) {
                    // Cara 2: Akses ViewPager2 secara langsung jika cara 1 gagal
                    try {
                        val viewPager = requireActivity().findViewById<ViewPager2>(R.id.fragment_container)
                        val nextPosition = 3 + (questionNumber + 1) // position = 3 + gadQuestionNumber
                        viewPager?.setCurrentItem(nextPosition, true)
                        Log.d(TAG, "Alternatif: Navigasi langsung ke position $nextPosition")
                    } catch (e2: Exception) {
                        Log.e(TAG, "Gagal navigasi ke pertanyaan berikutnya", e2)
                        Toast.makeText(context, "Gagal pindah ke pertanyaan berikutnya", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // TAMBAHAN: Observer sederhana yang hanya menunggu save selesai


    // TAMBAHAN: Simple loading state
    private fun showLoadingState(isLoading: Boolean) {
        if (isLoading) {
            binding.btnLanjutkan.isEnabled = false
            binding.btnLanjutkan.text = "Menyimpan..."
        } else {
            binding.btnLanjutkan.isEnabled = true
            binding.btnLanjutkan.text = if (isLastQuestion) "Selesai" else "Selanjutnya"
        }
    }


    // Override onDestroyView dengan logging tambahan
    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
        hasNavigated = false

    }

    companion object {
        private const val ARG_QUESTION_NUMBER = "question_number"
        private const val ARG_QUESTION_TEXT = "question_text"
        private const val ARG_IS_LAST = "is_last"
        private const val TAG = "GadQuestionFragment"
        private var isNavigatingToResult = false

        @JvmStatic
        fun newInstance(questionNumber: Int, questionText: String, isLast: Boolean = false) =
            GadQuestionFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_QUESTION_NUMBER, questionNumber)
                    putString(ARG_QUESTION_TEXT, questionText)
                    putBoolean(ARG_IS_LAST, isLast)
                }
            }
    }
}
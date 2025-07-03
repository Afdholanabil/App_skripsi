package com.healour.anxiety.ui.checkanxiety.formanxiety

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.healour.anxiety.R
import com.healour.anxiety.data.local.FormSessionManager
import com.healour.anxiety.databinding.FragmentActivityBinding
import com.healour.anxiety.ui.checkanxiety.FormAnxietyActivity
import com.healour.anxiety.ui.checkanxiety.FormAnxietyViewModel
import com.healour.anxiety.utils.ToastUtils
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ActivityFragment : Fragment() {
    private var _binding: FragmentActivityBinding? = null
    private val binding get() = _binding!!

    private lateinit var formSessionManager: FormSessionManager
    private var selectedCard: MaterialCardView? = null
    // Di EmotionFragment
    private lateinit var viewModel: FormAnxietyViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        formSessionManager = FormSessionManager(requireContext())
        // Dapatkan viewModel dari activity
        viewModel = (requireActivity() as FormAnxietyActivity).viewModel

        // Setup card view selection
        setupCardViewSelection()

        // Cek apakah sudah ada aktivitas yang dipilih sebelumnya
        MainScope().launch {
            val savedActivity = formSessionManager.activity.first()
            if (savedActivity.isNotEmpty()) {
                // Pilih card yang sesuai dengan aktivitas yang disimpan
                when (savedActivity) {
                    "Belajar/Bekerja" -> binding.cardStudyWork.performClick()
                    "Istirahat" -> binding.cardSleep.performClick()
                    "Hiburan" -> binding.cardEntertainment.performClick()
                    "Sosialisasi" -> binding.cardSocial.performClick()
                    "Olahraga" -> binding.cardSport.performClick()
                }
            }
        }

        // Di ActivityFragment, modifikasi btnLanjutkan onClick
        binding.btnLanjutkan.setOnClickListener {
            val selectedActivity = getSelectedActivity()
            if (selectedActivity == null) {
                ToastUtils.showToast(
                    requireContext(),
                    "Silakan pilih kegiatan terlebih dahulu",
                    duration = Toast.LENGTH_SHORT,
                    position = ToastUtils.Position.TOP
                )
                return@setOnClickListener
            }

            MainScope().launch {
                formSessionManager.saveActivity(selectedActivity)

                // Lanjut ke pertanyaan GAD-7 pertama
                val viewPager = activity?.findViewById<ViewPager2>(R.id.fragment_container)
                viewPager?.currentItem = 3 // Index 3 adalah pertanyaan GAD-7 pertama

                // Update indikator progress
                (activity as FormAnxietyActivity).updateProgressIndicator(3)
            }
        }
    }

    private fun setupCardViewSelection() {
        val cardViewsWithIcons = mapOf(
            binding.cardStudyWork to Pair(binding.icWork, binding.tvWork),
            binding.cardSleep to Pair(binding.icSleep, binding.tvSleep),
            binding.cardEntertainment to Pair(binding.icGame, binding.tvGame),
            binding.cardSocial to Pair(binding.icSocial, binding.tvSocial),
            binding.cardSport to Pair(binding.icSport, binding.tvSport)
        )

        cardViewsWithIcons.forEach { (cardView, iconTextPair) ->
            cardView.setOnClickListener {
                // Reset semua card ke inactive state
                resetAllCards(cardViewsWithIcons)

                // Set card yang dipilih ke active state
                setCardActive(cardView, iconTextPair.first, iconTextPair.second)

                // Simpan card yang dipilih
                selectedCard = cardView
            }
        }
    }

    private fun resetAllCards(cardViewsWithIcons: Map<MaterialCardView, Pair<ImageView, TextView>>) {
        cardViewsWithIcons.forEach { (cardView, iconTextPair) ->
            // Reset warna card
            cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray200))

            // Reset warna icon
            val icon = iconTextPair.first
            icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gray200))
            icon.setBackgroundResource(R.drawable.bg_icon_activity_form_inactive)

            // Reset warna text
            val text = iconTextPair.second
            text.setTextColor(ContextCompat.getColor(requireContext(), R.color.bluePrimary))
        }
    }

    private fun setCardActive(cardView: MaterialCardView, icon: ImageView, text: TextView) {
        // Set active card color
        cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bluePrimary))

        // Set active icon color and background
        icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.bluePrimary))
        icon.setBackgroundResource(R.drawable.bg_icon_activity_form)

        // Set active text color
        text.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
    }

    private fun getSelectedActivity(): String? {
        return when (selectedCard) {
            binding.cardStudyWork -> "Belajar/Bekerja"
            binding.cardSleep -> "Istirahat"
            binding.cardEntertainment -> "Hiburan"
            binding.cardSocial -> "Sosialisasi"
            binding.cardSport -> "Olahraga"
            else -> null
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ActivityFragment()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
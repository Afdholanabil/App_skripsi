package com.healour.anxiety.ui.profile.diary.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.healour.anxiety.data.firebase.FirebaseService
import com.healour.anxiety.data.repository.DiaryRepository
import com.healour.anxiety.databinding.FragmentDiaryListBinding
import com.healour.anxiety.ui.profile.diary.DiaryActivity
import com.healour.anxiety.ui.profile.diary.adapter.DiaryAdapter
import com.healour.anxiety.ui.profile.diary.DiaryViewModel
import com.healour.anxiety.ui.profile.diary.DiaryViewModelFactory


class DiaryListFragment : Fragment() {
    private var _binding: FragmentDiaryListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DiaryViewModel by viewModels {
        DiaryViewModelFactory(DiaryRepository(FirebaseService()))
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiaryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener {
            requireActivity().finish()
        }

        val diaryAdapter = DiaryAdapter(emptyList()) // Buat adapter dengan list kosong
        binding.recyclerViewDiary.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = diaryAdapter
        }

        // 🔥 Ambil data diary dari ViewModel
        viewModel.fetchDiaries()

        // 🔥 Observe perubahan data diary
        viewModel.diaries.observe(viewLifecycleOwner) { diaryList ->
            if (diaryList.isEmpty()) {
                binding.tvEmptyMessage.visibility = View.VISIBLE // Tampilkan pesan kosong
            } else {
                binding.tvEmptyMessage.visibility = View.GONE
                diaryAdapter.updateData(diaryList) // 🔥 Gunakan `DiffUtil` untuk update data
            }
        }

        binding.fabCreateDiary.setOnClickListener {
            (requireActivity() as DiaryActivity).navigateToCreateDiary()
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            DiaryListFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
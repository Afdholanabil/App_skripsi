package com.example.app_skripsi.ui.dashboard.profile

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.app_skripsi.databinding.FragmentProfileBinding
import com.example.app_skripsi.ui.dashboard.DashboardViewModel
import com.example.app_skripsi.ui.profile.AboutActivity
import com.example.app_skripsi.ui.profile.diary.DiaryActivity
import com.example.app_skripsi.ui.profile.FaqActivity
import com.example.app_skripsi.ui.profile.RiwayatCheckActivity
import com.example.app_skripsi.ui.profile.VideoListActivity

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // Ambil ViewModel dari DashboardActivity
    private val viewModel: DashboardViewModel by lazy {
        ViewModelProvider(requireActivity())[DashboardViewModel::class.java]
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
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Observe email user dari ViewModel agar selalu update
        viewModel.userName.observe(viewLifecycleOwner) { name ->

            binding.profileName.text = viewModel.getUserNameFromEmail(name)
            binding.profileInitial.text = viewModel.getAvatarInitial()
        }

        viewModel.userEmail.observe(viewLifecycleOwner) {email ->
            binding.profileEmail.text = email
        }

        setupNavigation()
    }

    private fun setupNavigation() {
        binding.menuDiary.setOnClickListener {
            startActivity(Intent(requireContext(), DiaryActivity::class.java))
        }
        binding.menuVideos.setOnClickListener {
            startActivity(Intent(requireContext(), VideoListActivity::class.java))
        }
        binding.menuHistory.setOnClickListener {
            startActivity(Intent(requireContext(), RiwayatCheckActivity::class.java))
        }
        binding.menuFaq.setOnClickListener {
            startActivity(Intent(requireContext(), FaqActivity::class.java))
        }
        binding.menuAbout.setOnClickListener {
            startActivity(Intent(requireContext(), AboutActivity::class.java))
        }

    }



    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProfileFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
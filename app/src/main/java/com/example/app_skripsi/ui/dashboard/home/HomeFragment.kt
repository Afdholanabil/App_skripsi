package com.example.app_skripsi.ui.dashboard.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.app_skripsi.R
import com.example.app_skripsi.databinding.FragmentHomeBinding
import com.example.app_skripsi.ui.dashboard.DashboardViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app_skripsi.data.model.VideoModel


class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by lazy {
        ViewModelProvider(requireActivity())[DashboardViewModel::class.java]
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observasi data dari ViewModel
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.tvGreeting.text = "${viewModel.getGreetingMessage()}, $name"
            binding.tvInitials.text = viewModel.getAvatarInitial()
        }

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val videoList = listOf(
            VideoModel("Video Yoga dalam 1 menit", "Yoga relaksasi dalam 1 menit.", R.drawable.video_thumbnail),
            VideoModel("Teknik Pernafasan", "Teknik pernafasan untuk meredakan kecemasan.", R.drawable.video_thumbnail),
            VideoModel("Meditasi Singkat", "Meditasi selama 5 menit untuk ketenangan.", R.drawable.video_thumbnail)
        )

        val videoAdapter = VideoAdapter(videoList)
        binding.recyclerViewVideo.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = videoAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {

            }
    }


}
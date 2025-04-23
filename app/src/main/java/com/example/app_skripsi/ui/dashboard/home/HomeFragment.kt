package com.example.app_skripsi.ui.dashboard.home

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.app_skripsi.R
import com.example.app_skripsi.databinding.FragmentHomeBinding
import com.example.app_skripsi.ui.dashboard.DashboardViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app_skripsi.data.firebase.FirebaseService
import com.example.app_skripsi.data.local.FormSessionManager
import com.example.app_skripsi.data.local.RoutineSessionManager
import com.example.app_skripsi.data.model.VideoModel
import com.example.app_skripsi.data.repository.VideoRepository
import com.example.app_skripsi.ui.checkanxiety.FormAnxietyActivity
import com.example.app_skripsi.ui.detailvideo.DetailVideoActivity
import com.example.app_skripsi.ui.profile.VideoListActivity
import com.example.app_skripsi.ui.video.VideoViewModel
import com.example.app_skripsi.ui.detailvideo.VideoViewModelFactory
import com.example.app_skripsi.utils.ToastUtils
import kotlinx.coroutines.launch
import android.util.Log

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by lazy {
        ViewModelProvider(requireActivity())[DashboardViewModel::class.java]
    }

    private val firebaseService by lazy { FirebaseService() }
    private val videoRepository by lazy { VideoRepository(firebaseService) }
    private val videoViewModel: VideoViewModel by viewModels {
        VideoViewModelFactory(videoRepository)
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

        // Setup greeting and UI
        setupUserGreeting()
        setupAnxietyCheckButtons()
        setupRecyclerView()
        observeVideoData()

        // Load all videos
        videoViewModel.loadAllVideos()
    }

    private fun setupUserGreeting() {
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            if (!name.isNullOrEmpty()) {
                binding.tvGreeting.text = "${viewModel.getGreetingMessage()}, $name"
                binding.tvInitials.text = name.firstOrNull()?.uppercase() ?: "U"
                Log.d("HomeFragment", "✅ Updated UI with name: $name")
            } else {
                Log.e("HomeFragment", "⚠️ Name is empty, using default")
                binding.tvGreeting.text = "${viewModel.getGreetingMessage()}, User"
                binding.tvInitials.text = "U"
            }
        }
    }

    private fun setupAnxietyCheckButtons() {
        binding.btnCheckAnxietyQuick.setOnClickListener {
            // Reset sesi sebelum memulai deteksi baru
            val formSessionManager = FormSessionManager(requireContext())
            lifecycleScope.launch {
                formSessionManager.resetSession()

                val intent = Intent(requireContext(), FormAnxietyActivity::class.java)
                intent.putExtra("DETECTION_TYPE", "QUICK")
                startActivity(intent)
            }
        }

        binding.btnCheckAnxietyRoutine.setOnClickListener {
            lifecycleScope.launch {
                // Periksa dulu apakah sudah mengisi hari ini
                val routineSessionManager = RoutineSessionManager(requireContext())
                val isSessionActive = routineSessionManager.isSessionStillActive()

                if (isSessionActive) {
                    val hasCompletedToday = routineSessionManager.hasCompletedFormToday()

                    if (hasCompletedToday) {
                        // Sudah mengisi form hari ini, tampilkan pesan
                        ToastUtils.showToast(
                            requireContext(),
                            "Anda sudah mengisi form deteksi kecemasan hari ini",
                            Toast.LENGTH_LONG,
                            position = ToastUtils.Position.TOP
                        )
                        return@launch
                    }
                }

                // Jika belum mengisi, lanjutkan dengan reset session dan buka FormAnxietyActivity
                val formSessionManager = FormSessionManager(requireContext())
                formSessionManager.resetSession()

                val intent = Intent(requireContext(), FormAnxietyActivity::class.java)
                intent.putExtra("DETECTION_TYPE", "ROUTINE")
                startActivity(intent)
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerViewVideo.layoutManager = LinearLayoutManager(requireContext())

        // Tambahkan onClick listener untuk "Video untuk anda" jika ingin mengarahkan ke VideoListActivity
        binding.tvVideoRekomendasi.setOnClickListener {
            val intent = Intent(requireContext(), VideoListActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeVideoData() {
        // Observe loading state
        videoViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.progressBar.visibility = View.GONE
            }
        }

        // Observe videos data
        videoViewModel.videos.observe(viewLifecycleOwner) { videos ->
            Log.d("HomeFragment", "Loaded ${videos.size} videos")

            if (videos.isNotEmpty()) {
                val videoAdapter = VideoAdapter(videos) { video ->
                    val intent = Intent(requireContext(), DetailVideoActivity::class.java)
                    intent.putExtra("VIDEO_MODEL", video)
                    startActivity(intent)
                }

                binding.recyclerViewVideo.adapter = videoAdapter
            } else {
                // Jika tidak ada video, tampilkan pesan
                showNoVideosMessage()
            }
        }

        // Observe error state
        videoViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Log.e("HomeFragment", "Error loading videos: $it")

                // Tampilkan pesan error khusus untuk masalah izin
                if (it.contains("PERMISSION_DENIED")) {
                    Toast.makeText(
                        requireContext(),
                        "Tidak dapat mengakses data video. Silakan cek aturan keamanan Firestore.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(requireContext(), "Gagal memuat video: $it", Toast.LENGTH_SHORT).show()
                }

                videoViewModel.clearError()

                // Tampilkan UI kosong
                showNoVideosMessage()
            }
        }
    }

    private fun showNoVideosMessage() {
        // Tampilkan pesan bahwa tidak ada video yang tersedia
        Toast.makeText(
            requireContext(),
            "Tidak ada video tersedia saat ini",
            Toast.LENGTH_SHORT
        ).show()

        // Opsional: Tampilkan UI kosong jika Anda memiliki view untuk itu
         binding.tvEmptyMessage.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }
}
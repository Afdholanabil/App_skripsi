package com.example.app_skripsi.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app_skripsi.R
import com.example.app_skripsi.data.firebase.FirebaseService
import com.example.app_skripsi.data.repository.VideoRepository
import com.example.app_skripsi.databinding.ActivityVideoListBinding
import com.example.app_skripsi.ui.dashboard.home.VideoAdapter
import com.example.app_skripsi.ui.detailvideo.DetailVideoActivity
import com.example.app_skripsi.ui.detailvideo.VideoViewModelFactory
import com.example.app_skripsi.ui.video.VideoViewModel
import android.util.Log

class VideoListActivity : AppCompatActivity() {
    private var _binding : ActivityVideoListBinding? = null
    private val binding get() = _binding!!

    private val firebaseService by lazy { FirebaseService() }
    private val videoRepository by lazy { VideoRepository(firebaseService) }
    private val viewModel: VideoViewModel by viewModels {
        VideoViewModelFactory(videoRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityVideoListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bottomNav = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left+v.paddingLeft , systemBars.top + v.paddingTop,
                systemBars.right + v.paddingRight , systemBars.bottom + v.paddingBottom)
            insets
        }

        // Hide status bar & navigation bar
        hideSystemUI()

        // Setup RecyclerView
        setupRecyclerView()

        // PENTING: Panggil metode untuk mengobservasi perubahan data
        observeViewModel()

        // Tentukan kategori dari intent jika ada, atau load semua video
        val category = intent.getStringExtra("VIDEO_CATEGORY")
        if (category != null) {
            binding.tvTitle.text = "Video $category"
            viewModel.loadVideosByCategory(category)
        } else {
            binding.tvTitle.text = "Semua Video"
            viewModel.loadAllVideos()
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        binding.recyclerViewVideo.layoutManager = LinearLayoutManager(this)
    }

    private fun observeViewModel() {
        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe videos data
        viewModel.videos.observe(this) { videos ->
            Log.d("VideoListActivity", "Received ${videos.size} videos from ViewModel")

            if (videos.isNotEmpty()) {
                val videoAdapter = VideoAdapter(videos) { video ->
                    val intent = Intent(this, DetailVideoActivity::class.java)
                    intent.putExtra("VIDEO_MODEL", video)
                    startActivity(intent)
                }
                binding.recyclerViewVideo.adapter = videoAdapter
            } else {
                // Tampilkan pesan tidak ada video
                Toast.makeText(this, "Tidak ada video tersedia", Toast.LENGTH_SHORT).show()
            }
        }

        // Observe error state
        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Log.e("VideoListActivity", "Error loading videos: $it")

                // Tampilkan pesan error khusus untuk masalah izin
                if (it.contains("PERMISSION_DENIED")) {
                    Toast.makeText(
                        this,
                        "Tidak dapat mengakses data video. Silakan cek aturan keamanan Firestore.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(this, "Gagal memuat video: $it", Toast.LENGTH_SHORT).show()
                }

                viewModel.clearError()
            }
        }
    }

    private fun hideSystemUI() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
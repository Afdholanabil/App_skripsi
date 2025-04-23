package com.example.app_skripsi.ui.profile

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app_skripsi.databinding.ActivityRiwayatCheckBinding
import com.example.app_skripsi.data.firebase.FirebaseService
import com.example.app_skripsi.data.repository.AnxietyRepository
import com.example.app_skripsi.ui.checkanxiety.shortdetection.ShortDetectionAdapter
import com.example.app_skripsi.ui.checkanxiety.shortdetection.ShortDetectionViewModel
import com.example.app_skripsi.ui.checkanxiety.shortdetection.ShortDetectionViewModelFactory
import kotlinx.coroutines.launch

class RiwayatCheckActivity : AppCompatActivity() {

    private var _binding: ActivityRiwayatCheckBinding? = null
    private val binding get() = _binding!!

    // Mendeklarasikan ViewModel menggunakan delegasi viewModels
    private lateinit var firebaseService: FirebaseService
    private val shortDetectionViewModel: ShortDetectionViewModel by viewModels {
        // Pastikan firebaseService sudah diinisialisasi di sini
        ShortDetectionViewModelFactory(AnxietyRepository(firebaseService)) // Pastikan untuk menyuplai AnxietyRepository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityRiwayatCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi firebaseService
        firebaseService = FirebaseService() // Atau FirebaseService.getInstance(), sesuai cara kamu menginisialisasinya

        // Setup RecyclerView
        val recyclerView = binding.recyclerViewVideo
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Observe LiveData dari ViewModel untuk mendapatkan data deteksi singkat
        shortDetectionViewModel.shortDetectionList.observe(this) { shortDetectionList ->
            // Mengatur visibilitas berdasarkan ada atau tidaknya data
            if (shortDetectionList.isNotEmpty()) {
                // Menyembunyikan ProgressBar dan menampilkan RecyclerView
                binding.progressBar.visibility = android.view.View.GONE
                binding.recyclerViewVideo.visibility = android.view.View.VISIBLE
                binding.tvNoData.visibility = android.view.View.GONE

                // Mengatur adapter untuk RecyclerView
                val adapter = ShortDetectionAdapter(shortDetectionList)
                recyclerView.adapter = adapter
            } else {
                // Jika tidak ada data, menampilkan pesan kosong
                binding.progressBar.visibility = android.view.View.GONE
                binding.tvNoData.visibility = android.view.View.VISIBLE
                binding.recyclerViewVideo.visibility = android.view.View.GONE
            }
        }

        // Menampilkan ProgressBar saat data sedang dimuat
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.recyclerViewVideo.visibility = android.view.View.GONE
        binding.tvNoData.visibility = android.view.View.GONE

        // Meminta ViewModel untuk mengambil data deteksi singkat
        lifecycleScope.launch {
            shortDetectionViewModel.fetchShortDetections() // Memanggil fungsi suspend dalam ViewModel
        }
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}

package com.example.app_skripsi.ui.dashboard.checkanxiety

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.app_skripsi.R
import com.example.app_skripsi.data.firebase.FirebaseService
import com.example.app_skripsi.data.local.RoutineSessionManager
import com.example.app_skripsi.databinding.FragmentCheckAnxietyBinding
import com.example.app_skripsi.ui.checkanxiety.FormAnxietyActivity
import com.example.app_skripsi.utils.ToastUtils
import kotlinx.coroutines.launch

class CheckAnxietyFragment : Fragment() {
    private var _binding: FragmentCheckAnxietyBinding? = null
    private val binding get() = _binding!!
    private lateinit var routineSessionManager: RoutineSessionManager
    private lateinit var firebaseService: FirebaseService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // Handle any arguments here if needed
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckAnxietyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        routineSessionManager = RoutineSessionManager(requireContext())
        firebaseService = FirebaseService()

        // Setup tombol mulai/lanjutkan sesi
        binding.btnCheckAnxietyRoutine.setOnClickListener {
            handleRoutineButtonClick()
        }

        // Setup tombol akhiri sesi
        binding.btnEndAnxietyRoutine.setOnClickListener {
            showEndSessionConfirmationDialog()
        }

        // Cek status sesi rutin setiap kali fragment muncul
        checkRoutineSessionStatus()
    }

    override fun onResume() {
        super.onResume()
        // Update status sesi ketika fragment kembali muncul
        checkRoutineSessionStatus()
    }
    private fun showEndSessionConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Akhiri Sesi Deteksi Rutin")
            .setMessage("Apakah Anda yakin ingin mengakhiri sesi deteksi rutin saat ini? Data yang sudah ada akan tetap tersimpan.")
            .setPositiveButton("Ya, Akhiri") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val success = routineSessionManager.endRoutineSession(firebaseService)

                        if (success) {
                            Toast.makeText(requireContext(),
                                "Sesi deteksi rutin berhasil diakhiri",
                                Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(),
                                "Gagal mengakhiri sesi deteksi rutin",
                                Toast.LENGTH_SHORT).show()
                        }

                        // Refresh status setelah operasi
                        checkRoutineSessionStatus()
                    } catch (e: Exception) {
                        Log.e("CheckAnxietyFragment", "Error saat mengakhiri sesi: ${e.message}")
                        Toast.makeText(requireContext(),
                            "Terjadi kesalahan saat mengakhiri sesi",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun handleRoutineButtonClick() {
        lifecycleScope.launch {
            try {
                // Periksa apakah user sudah login
                val userId = firebaseService.getCurrentUserId()
                if (userId.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), "Anda belum login", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Simpan user ID ke RoutineSessionManager
                routineSessionManager.setUserId(userId)

                // Periksa sesi aktif dan status pengisian
                val isSessionActive = routineSessionManager.isSessionStillActive()

                if (isSessionActive) {
                    val hasCompletedToday = routineSessionManager.hasCompletedFormToday()

                    if (hasCompletedToday) {
                        ToastUtils.showToast(
                            requireContext(),
                            "Anda sudah mengisi form deteksi kecemasan hari ini",
                            Toast.LENGTH_LONG,
                            position = ToastUtils.Position.TOP
                        )
                        return@launch
                    }
                }

                // Jika belum mengisi atau tidak ada sesi aktif, buka FormAnxietyActivity
                val intent = Intent(requireContext(), FormAnxietyActivity::class.java)
                intent.putExtra("DETECTION_TYPE", "ROUTINE")
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("CheckAnxietyFragment", "Error menangani klik tombol: ${e.message}")
                Toast.makeText(requireContext(), "Terjadi kesalahan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkRoutineSessionStatus() {
        lifecycleScope.launch {
            try {
                // Dapatkan user ID saat ini
                val userId = firebaseService.getCurrentUserId()
                if (userId == null) {
                    updateUIForNoSession()
                    return@launch
                }

                // Simpan user ID ke RoutineSessionManager
                routineSessionManager.setUserId(userId)

                val isSessionActive = routineSessionManager.isSessionStillActive()
                Log.d("CheckAnxietyFragment", "Session active: $isSessionActive")

                if (isSessionActive) {
                    // Sesi aktif, tampilkan informasi sesi
                    val sessionType = routineSessionManager.getSessionTypeDisplay()
                    val currentDay = routineSessionManager.getCurrentSessionDay()
                    val totalDays = routineSessionManager.getSessionDurationInDays()
                    val hasCompletedToday = routineSessionManager.hasCompletedFormToday()

                    Log.d("CheckAnxietyFragment",
                        "Session info - Type: $sessionType, Day: $currentDay/$totalDays, Completed today: $hasCompletedToday")

                    binding.tvTitle.text = "Deteksi Rutin"
                    binding.tvDescAnxiety.text = "Sesi $sessionType aktif"
                    binding.tvDayAnxiety.visibility = View.VISIBLE
                    binding.tvDayAnxiety.text = "Hari $currentDay dari $totalDays"

                    // Tampilkan tombol akhiri sesi
                    binding.btnEndAnxietyRoutine.visibility = View.VISIBLE

                    if (hasCompletedToday) {
                        binding.btnCheckAnxietyRoutine.text = "Sudah Diisi Hari Ini"
                        binding.tvCheckAnxietyPeriodic.text = "Anda sudah mengisi form deteksi kecemasan hari ini"
                        binding.btnCheckAnxietyRoutine.isEnabled = false
                        binding.btnCheckAnxietyRoutine.setBackgroundColor(
                            ContextCompat.getColor(requireContext(), R.color.gray400)
                        )
                        binding.btnCheckAnxietyRoutine.setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.gray700)
                        )
                    } else {
                        binding.btnCheckAnxietyRoutine.text = "Lanjutkan Sesi"
                        binding.btnCheckAnxietyRoutine.isEnabled = true
                        binding.tvCheckAnxietyPeriodic.text = "Lanjutkan sesi deteksi kecemasan rutin Anda"
                        binding.btnCheckAnxietyRoutine.setBackgroundColor(
                            ContextCompat.getColor(requireContext(), R.color.bluePrimary)
                        )
                        binding.btnCheckAnxietyRoutine.setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.white)
                        )
                    }
                } else {
                    updateUIForNoSession()
                }
            } catch (e: Exception) {
                Log.e("CheckAnxietyFragment", "Error checking routine session status", e)
                updateUIForNoSession()
            }
        }
    }

    private fun updateUIForNoSession() {
        binding.tvTitle.text = "Deteksi Rutin"
        binding.tvDescAnxiety.text = "Anda belum memulai sesi Deteksi Rutin !"
        binding.tvDayAnxiety.visibility = View.GONE

        binding.btnCheckAnxietyRoutine.text = "Mulai Sesi"
        binding.btnCheckAnxietyRoutine.isEnabled = true
        binding.tvCheckAnxietyPeriodic.text = "Apakah anda ingin memulai sesi rutin untuk deteksi kecemasan anda?"
        binding.btnCheckAnxietyRoutine.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.bluePrimary)
        )
        binding.btnCheckAnxietyRoutine.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.white)
        )

        // Sembunyikan tombol akhiri sesi
        binding.btnEndAnxietyRoutine.visibility = View.GONE
    }



    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            CheckAnxietyFragment().apply {
                arguments = Bundle().apply {
                    // Setup arguments if needed
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
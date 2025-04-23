package com.example.app_skripsi.ui.checkanxiety.formanxiety

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.app_skripsi.R
import com.example.app_skripsi.data.local.FormSessionManager
import com.example.app_skripsi.databinding.FragmentPermissionBinding
import com.example.app_skripsi.ui.checkanxiety.FormAnxietyActivity
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch


class PermissionFragment : Fragment() {
    private var _binding : FragmentPermissionBinding? = null
    private val binding get() = _binding!!

    private lateinit var formSessionManager: FormSessionManager
    private val TAG = "PermissionFragment" // Tag untuk logging

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPermissionBinding.inflate(inflater, container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        formSessionManager = FormSessionManager(requireContext())


        // Pastikan untuk mendapatkan tipe deteksi LANGSUNG dari activity
        val detectionType = (activity as? FormAnxietyActivity)?.detectionType ?: "QUICK"
        Log.d(TAG, "Detection type from Activity: $detectionType")

        // Update deskripsi berdasarkan tipe dari activity
        updateDescriptionBasedOnType(detectionType)

        // Juga periksa dari DataStore untuk memastikan konsistensi
        lifecycleScope.launch {
            val storedType = formSessionManager.getDetectionType()
            Log.d(TAG, "Detection type from DataStore: $storedType")

            // Jika ada perbedaan, update lagi deskripsi
            if (storedType != detectionType) {
                Log.d(TAG, "Type mismatch! Updating from DataStore value")
                updateDescriptionBasedOnType(storedType)
            }
        }

//        // Tentukan tipe deteksi dan perbarui deskripsi
//        lifecycleScope.launch {
//            val detectionType = formSessionManager.getDetectionType()
//            updateDescriptionBasedOnType(detectionType)
//
//            // Log tipe deteksi saat fragment dibuat
//            Log.d(TAG, "Detection type on fragment creation: $detectionType")
//        }

        binding.btnLanjutkan.setOnClickListener {
            lifecycleScope.launch {
                // Log tipe deteksi saat tombol diklik
                val detectionType = formSessionManager.getDetectionType()
                Log.d(TAG, "Detection type when button clicked: $detectionType")

                // Simpan langkah saat ini sebagai "emotion" untuk navigasi
                formSessionManager.saveEmotion("")

                // Pindah ke halaman berikutnya dengan ViewPager2
                val viewPager2 = activity?.findViewById<ViewPager2>(R.id.fragment_container)
                viewPager2?.currentItem = 1 // Pindah ke fragment EmotionFragment (index 1)
            }
        }
    }

    // Fungsi untuk memperbarui deskripsi berdasarkan tipe deteksi
    private fun updateDescriptionBasedOnType(detectionType: String) {
        if (detectionType == "ROUTINE") {
            binding.tvPermissionDescription.text = getString(R.string.routine_detection_description)
            Log.d(TAG, "Setting description for ROUTINE detection")
        } else {
            binding.tvPermissionDescription.text = getString(R.string.quick_detection_description)
            Log.d(TAG, "Setting description for QUICK detection")
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            PermissionFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }
}
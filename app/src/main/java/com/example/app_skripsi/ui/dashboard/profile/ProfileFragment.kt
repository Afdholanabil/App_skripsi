package com.example.app_skripsi.ui.dashboard.profile

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.app_skripsi.data.firebase.FirebaseService
import com.example.app_skripsi.data.local.SessionManager
import com.example.app_skripsi.data.local.AppDatabase
import com.example.app_skripsi.data.local.user.toUserModel
import com.example.app_skripsi.data.repository.UserRepository
import com.example.app_skripsi.databinding.FragmentProfileBinding
import com.example.app_skripsi.ui.auth.login.LoginActivity
import com.example.app_skripsi.ui.dashboard.DashboardViewModel
import com.example.app_skripsi.ui.profile.AboutActivity
import com.example.app_skripsi.ui.profile.diary.DiaryActivity
import com.example.app_skripsi.ui.profile.faq.FaqActivity
import com.example.app_skripsi.ui.profile.RiwayatCheckActivity
import com.example.app_skripsi.ui.profile.VideoListActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // Ambil ViewModel dari DashboardActivity
    private val viewModel: DashboardViewModel by lazy {
        ViewModelProvider(requireActivity())[DashboardViewModel::class.java]
    }
    private val sessionManager by lazy { SessionManager(requireContext()) }

    // ✅ Initialize userRepository properly
    private val userRepository by lazy {
        val database = AppDatabase.getDatabase(requireContext())
        UserRepository(FirebaseService(), database.userDao())
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

        viewModel.userEmail.observe(viewLifecycleOwner) { email ->
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
        binding.menuLogout.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val userId = sessionManager.sessionUserId.first()
                if (userId == null) {
                    android.util.Log.e("LogoutFlow", "❌ sessionUserId is NULL, cannot logout")
                    return@launch
                }


                // ✅ Check for data changes in SQLite before logging out
                val localUser = userRepository.getUserFromLocal(userId)
                val firebaseUser = userRepository.getUserFromFirebase(userId)

                if (localUser != null && firebaseUser.isSuccess) {
                    val firebaseData = firebaseUser.getOrNull()
                    if (firebaseData != localUser.toUserModel()) {
                        android.util.Log.d("LogoutFlow", "🔄 Data has changed, syncing to Firebase...")

                        // ✅ Sync to Firebase
                        val updateResult = userRepository.updateUserToFirebase(localUser)
                        if (updateResult.isSuccess) {
                            android.util.Log.d("LogoutFlow", "✅ Data Synced to Firebase before logout")
                        } else {
                            android.util.Log.e("LogoutFlow", "❌ Failed to sync data to Firebase before logout")
                        }
                    } else {
                        android.util.Log.d("LogoutFlow", "🔹 No changes detected, skipping Firebase sync")
                    }
                }

                // ✅ Clear session & SQLite
                sessionManager.clearSession()
                userRepository.clearLocalDatabase()

                android.util.Log.d("LogoutFlow", "🗑 SQLite Cleared & Session Ended")

                val intent = Intent(requireContext(), LoginActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

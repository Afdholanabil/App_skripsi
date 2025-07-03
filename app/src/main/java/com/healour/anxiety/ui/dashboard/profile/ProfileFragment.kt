package com.healour.anxiety.ui.dashboard.profile

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.healour.anxiety.data.firebase.FirebaseService
import com.healour.anxiety.data.local.SessionManager
import com.healour.anxiety.data.local.AppDatabase
import com.healour.anxiety.data.local.FormSessionManager
import com.healour.anxiety.data.local.RoutineSessionManager
import com.healour.anxiety.data.local.user.toUserModel
import com.healour.anxiety.data.repository.UserRepository
import com.healour.anxiety.databinding.FragmentProfileBinding
import com.healour.anxiety.ui.auth.login.LoginActivity
import com.healour.anxiety.ui.dashboard.DashboardViewModel
import com.healour.anxiety.ui.profile.AboutActivity
import com.healour.anxiety.ui.profile.diary.DiaryActivity
import com.healour.anxiety.ui.profile.faq.FaqActivity
import com.healour.anxiety.ui.profile.RiwayatCheckActivity
import com.healour.anxiety.ui.profile.VideoListActivity
import com.healour.anxiety.utils.NotificationSchedulerManager
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
        // Di dalam ProfileFragment.kt, perbarui bagian handleLogout/menuLogout:
        binding.menuLogout.setOnClickListener {
            performLogout()
        }

    }

    private fun performLogout() {
        // Tampilkan confirmation dialog terlebih dahulu
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Logout")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya") { _, _ ->
                startLogoutProcess()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun startLogoutProcess() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLogoutLoading(true)
                val userId = sessionManager.sessionUserId.first()
                if (userId == null) {
                    android.util.Log.e("LogoutFlow", "❌ sessionUserId is NULL, cannot logout")
                    showLogoutLoading(false)
                    return@launch
                }

                android.util.Log.d("LogoutFlow", "🔄 Starting logout process for user: $userId")

                try {
                    val localUser = userRepository.getUserFromLocal(userId)
                    val firebaseUser = userRepository.getUserFromFirebase(userId)

                    if (localUser != null && firebaseUser.isSuccess) {
                        val firebaseData = firebaseUser.getOrNull()
                        if (firebaseData != localUser.toUserModel()) {
                            android.util.Log.d("LogoutFlow", "🔄 Data has changed, syncing to Firebase...")

                            val updateResult = userRepository.updateUserToFirebase(localUser)
                            if (updateResult.isSuccess) {
                                android.util.Log.d("LogoutFlow", "✅ Data Synced to Firebase before logout")
                            } else {
                                android.util.Log.e("LogoutFlow", "❌ Failed to sync data to Firebase before logout")
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LogoutFlow", "Error during data sync: ${e.message}")
                }

                try {
                    val notificationManager = NotificationSchedulerManager(requireContext())
                    notificationManager.cancelRoutineFormAlarms()
                    notificationManager.cancelAlarmNotifications()
                } catch (e: Exception) {
                    android.util.Log.e("LogoutFlow", "Error cancelling notifications: ${e.message}")
                }

                try {
                    val routineSessionManager = RoutineSessionManager(requireContext())
                    routineSessionManager.clearAllData()

                    val formSessionManager = FormSessionManager(requireContext())
                    formSessionManager.resetSession()
                } catch (e: Exception) {
                    android.util.Log.e("LogoutFlow", "Error clearing session data: ${e.message}")
                }

                try {
                    sessionManager.clearSession()
                    userRepository.clearLocalDatabase()
                } catch (e: Exception) {
                    android.util.Log.e("LogoutFlow", "Error clearing database: ${e.message}")
                }

                val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                requireActivity().finish()

            } catch (e: Exception) {
                android.util.Log.e("LogoutFlow", "Unexpected error during logout: ${e.message}", e)
                showLogoutLoading(false)
            }
        }
    }

    private fun showLogoutLoading(show: Boolean) {
        if (show) {
            // Disable logout menu item
            binding.menuLogout.isEnabled = false
            binding.menuLogout.alpha = 0.6f

            // Show progress indicator jika ada
            binding.progressBar.visibility = View.VISIBLE

            // Disable other menu items selama logout
            binding.menuDiary.isEnabled = false
            binding.menuVideos.isEnabled = false
            binding.menuHistory.isEnabled = false
            binding.menuFaq.isEnabled = false
            binding.menuAbout.isEnabled = false

        } else {
            // Re-enable logout menu
            binding.menuLogout.isEnabled = true
            binding.menuLogout.alpha = 1.0f

            // Hide progress indicator
            binding.progressBar.visibility = View.GONE

            // Re-enable other menu items
            binding.menuDiary.isEnabled = true
            binding.menuVideos.isEnabled = true
            binding.menuHistory.isEnabled = true
            binding.menuFaq.isEnabled = true
            binding.menuAbout.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

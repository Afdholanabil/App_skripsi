package com.example.app_skripsi.ui.auth.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.app_skripsi.MainActivity
import com.example.app_skripsi.R
import com.example.app_skripsi.data.firebase.FirebaseService
import com.example.app_skripsi.data.local.AppDatabase
import com.example.app_skripsi.data.local.SessionManager
import com.example.app_skripsi.data.local.user.UserDao
import com.example.app_skripsi.data.local.user.UserEntity
import com.example.app_skripsi.data.repository.AuthRepository
import com.example.app_skripsi.data.repository.UserRepository
import com.example.app_skripsi.databinding.ActivityLoginBinding
import com.example.app_skripsi.ui.auth.AuthViewModelFactory
import com.example.app_skripsi.ui.auth.forgotPw.ForgotPwActivity
import com.example.app_skripsi.ui.auth.register.RegisterActivity
import com.example.app_skripsi.ui.dashboard.DashboardActivity
import com.example.app_skripsi.utils.ToastUtils
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {
    private var _binding : ActivityLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels {
        val database = AppDatabase.getDatabase(application) // Get Room database instance
        val userDao = database.userDao() // Get UserDao from database

        LoginViewModelFactory(
            application,
            AuthRepository(FirebaseService()),
            UserRepository(FirebaseService(), userDao)
        )
    }


    private var email:String? = null

    private var backPresssedTime : Long= 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }

        binding.signupLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.loginButton.setOnClickListener {
            email = binding.emailField.text.toString().trim()
            val password = binding.passwordField.text.toString().trim()

            if (email.isNullOrEmpty() || password.isEmpty()) {
                ToastUtils.showToast(this, "Email dan Password harus diisi", position = ToastUtils.Position.TOP)
            } else {
                viewModel.login(email!!, password)
            }
        }

        binding.forgotPasswordLink.setOnClickListener {
            val intent = Intent( this, ForgotPwActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

        }

        binding.tlPasswordField.setEndIconOnClickListener {
            val isVisible = binding.passwordField.inputType != android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.passwordField.inputType = if (isVisible) {
                android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            binding.passwordField.setSelection(binding.passwordField.text?.length ?: 0)

            // Ubah ikon saat state berubah
            binding.tlPasswordField.endIconDrawable = if (isVisible) {
                ContextCompat.getDrawable(this, R.drawable.hide)
            } else {
                ContextCompat.getDrawable(this, R.drawable.show)
            }
        }

        observeViewModel()
        setupTextWatchers()
        setButtonState()
    }

    private fun observeViewModel() {
        viewModel.loginResult.observe(this, Observer { result ->
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user != null) {
                    ToastUtils.showToast(this, "Login berhasil!", position = ToastUtils.Position.BOTTOM)

                    saveSessionAndNavigate(user)
//                    val intent = Intent(this, DashboardActivity::class.java).apply {
//                        putExtra("USER_NAME", user.nama)
//                        putExtra("USER_EMAIL", user.email)
//                        putExtra("USER_GENDER", user.jenisKelamin)
//                        putExtra("USER_AGE", user.umur)
//                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
//                    }
//                    startActivity(intent)
//                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
//                    finish()
                }
            } else {
                ToastUtils.showToast(this, "Login gagal: ${result.exceptionOrNull()?.message}", position = ToastUtils.Position.TOP)
                Log.e(TAG,"Login gagal: ${result.exceptionOrNull()?.message}")
            }
        })
    }

    private fun saveSessionAndNavigate(user: UserEntity) {
        // Simpan session userId dan token
        val userId = user.userId
        val token = "dummy_token"  // Ganti dengan token yang didapat dari Firebase
        val expiresAt = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(24)

        lifecycleScope.launch {
            // Simpan session di SessionManager
            val sessionManager = SessionManager(application)
            sessionManager.saveSession(token, userId, expiresAt)

            // Pindah ke DashboardActivity dengan membawa data userId
            val intent = Intent(this@LoginActivity, DashboardActivity::class.java).apply {
                putExtra("USER_ID", userId)
            }
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    // ✅ Helper function untuk validasi field
    private fun isAllFieldsFilled(): Boolean {
        return binding.emailField.text.toString().isNotEmpty() &&
                binding.passwordField.text.toString().isNotEmpty()
    }

    private fun setButtonState() {
        binding.loginButton.isEnabled = isAllFieldsFilled()
    }

    private fun setupTextWatchers() {
        binding.emailField.addTextChangedListener { setButtonState() }
        binding.passwordField.addTextChangedListener { setButtonState() }
    }

    // Handle double back press untuk keluar aplikasi
    override fun onBackPressed() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPresssedTime <= 2000) {
            super.onBackPressed()  // Keluar aplikasi
        } else {
            backPresssedTime = currentTime
            Toast.makeText(this, "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null  // Menghapus binding untuk menghindari memory leak
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}

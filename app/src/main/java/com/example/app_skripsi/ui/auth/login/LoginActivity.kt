package com.example.app_skripsi.ui.auth.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import com.example.app_skripsi.MainActivity
import com.example.app_skripsi.R
import com.example.app_skripsi.databinding.ActivityLoginBinding
import com.example.app_skripsi.ui.auth.register.RegisterActivity

class LoginActivity : AppCompatActivity() {
    private var _binding : ActivityLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel : LoginViewModel by viewModels()

    private var backPresssedTime : Long= 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.signupLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.loginButton.setOnClickListener {
            val email = binding.emailField.text.toString().trim()
            val password = binding.passwordField.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                showToast("Email dan Password harus diisi")
            } else {
                viewModel.login(email, password)
            }
        }
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.loginResult.observe(this, Observer { result ->
            if (result.isSuccess) {
                showToast("Login berhasil!")
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                })
                finish()
            } else {
                showToast("Login gagal: ${result.exceptionOrNull()?.message}")
            }
        })

        viewModel.loading.observe(this, Observer { isLoading ->
            binding.loginButton.isEnabled = !isLoading
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
}
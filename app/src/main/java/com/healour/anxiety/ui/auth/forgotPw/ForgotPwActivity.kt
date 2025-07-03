package com.healour.anxiety.ui.auth.forgotPw

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import com.healour.anxiety.R
import com.healour.anxiety.data.firebase.FirebaseService
import com.healour.anxiety.data.repository.AuthRepository
import com.healour.anxiety.databinding.ActivityForgotPwBinding
import com.healour.anxiety.ui.auth.AuthViewModelFactory
import com.healour.anxiety.utils.ToastUtils

class ForgotPwActivity : AppCompatActivity() {
    private var _binding : ActivityForgotPwBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ForgotPasswordViewModel by viewModels {
        AuthViewModelFactory(AuthRepository(FirebaseService()))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityForgotPwBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding()
            v.setPadding(systemBars.left + v.paddingLeft, systemBars.top ,
                systemBars.right + v.paddingRight, systemBars.bottom )
            insets
        }

        binding.resetPasswordButton.setOnClickListener {
            val email = binding.emailFPField.text.toString().trim()
            if (email.isEmpty()) {
                ToastUtils.showToast(this, "Email harus diisi", position = ToastUtils.Position.TOP)
            } else {
                viewModel.sendPasswordReset(email)
            }
        }

        observeViewModel()
        setupTextWatchers()
    }

    private fun observeViewModel() {
        viewModel.loading.observe(this) {isLoading ->
            showLoading(isLoading)
        }
        viewModel.resetResult.observe(this, Observer { result ->
            if (result.isSuccess) {
                ToastUtils.showToast(
                    this,
                    "Link reset password telah dikirim ke email Anda.",
                    position = ToastUtils.Position.TOP
                )
                finish()
            } else {
                ToastUtils.showToast(
                    this,
                    "Gagal mengirim link: ${result.exceptionOrNull()?.message}",
                    position = ToastUtils.Position.TOP
                )
            }
        })

    }

    private fun showLoading(isLoading :Boolean){
        if (isLoading){
            binding.progressCircular.visibility = View.VISIBLE
        }else {
            binding.progressCircular.visibility = View.GONE
        }
    }

    private fun setButtonState() {
        val isAllFieldFilled = binding.emailFPField.text.toString().isNotEmpty()

        binding.resetPasswordButton.isEnabled = isAllFieldFilled
    }

    private fun setupTextWatchers() {
        binding.emailFPField.addTextChangedListener { setButtonState() }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null  // Menghapus binding untuk menghindari memory leak
    }
}
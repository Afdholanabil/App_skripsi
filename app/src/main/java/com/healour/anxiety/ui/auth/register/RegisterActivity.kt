package com.healour.anxiety.ui.auth.register

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import com.healour.anxiety.R
import com.healour.anxiety.data.firebase.FirebaseService
import com.healour.anxiety.data.model.UserModel
import com.healour.anxiety.data.repository.AuthRepository
import com.healour.anxiety.databinding.ActivityRegisterBinding
import com.healour.anxiety.ui.auth.AuthViewModelFactory
import com.healour.anxiety.ui.auth.login.LoginActivity
import com.healour.anxiety.utils.ToastUtils

class RegisterActivity : AppCompatActivity() {
    private var _binding : ActivityRegisterBinding? = null
    private val binding get() =_binding!!

    private val viewModel: RegisterViewModel by viewModels {
        AuthViewModelFactory(AuthRepository(FirebaseService()))
    }
    private var btnState : Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding =ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this) {
            showBackConfirmationDialog()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
        // Navigasi kembali ke LoginActivity
        binding.signInLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            finish()
        }
        binding.registerBtn.setOnClickListener {
            val email = binding.emailField.text.toString().trim()
            val password = binding.passwordField.text.toString().trim()
            val confirmPassword = binding.confirmPasswordField.text.toString().trim()
            val name = binding.nameField.text.toString().trim()
            val gender = binding.actvGender.text.toString().trim()
            val age = binding.ageField.text.toString().trim().toIntOrNull() ?: 0

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() ||
                name.isEmpty() || gender.isEmpty() || age == 0) {
                ToastUtils.showToast(this, "Semua field harus diisi", position = ToastUtils.Position.TOP)
                return@setOnClickListener
            }

            // 🔹 Pastikan password valid
            if (!isValidPassword(password)) {
                ToastUtils.showToast(this, "Password harus minimal 8 karakter, ada huruf kapital, angka, dan simbol.", position = ToastUtils.Position.TOP)
                return@setOnClickListener
            }

            // 🔹 Pastikan password dan konfirmasi password cocok
            if (password != confirmPassword) {
                ToastUtils.showToast(this, "Konfirmasi password tidak cocok", position = ToastUtils.Position.TOP)
                return@setOnClickListener
            }

            val user = UserModel(
                nama = name,
                email = email,
                jenisKelamin = gender,
                umur = age
            )
            viewModel.register(email, password, user)
        }



        val gender = resources.getStringArray(R.array.gender_array)
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, gender)
        binding!!.actvGender.setAdapter(genderAdapter)

        binding.tlPassword.setEndIconOnClickListener {
            val isVisible = binding.passwordField.inputType != android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.passwordField.inputType = if (isVisible) {
                android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            binding.passwordField.setSelection(binding.passwordField.text?.length ?: 0)

            // Ubah ikon saat state berubah
            binding.tlPassword.endIconDrawable = if (isVisible) {
                ContextCompat.getDrawable(this, R.drawable.hide)
            } else {
                ContextCompat.getDrawable(this, R.drawable.show)
            }
        }

        binding.tlConfirmPassword.setEndIconOnClickListener {
            val isVisible = binding.confirmPasswordField.inputType != android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.confirmPasswordField.inputType = if (isVisible) {
                android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            binding.confirmPasswordField.setSelection(binding.confirmPasswordField.text?.length ?: 0)

            // Ubah ikon saat state berubah
            binding.tlConfirmPassword.endIconDrawable = if (isVisible) {
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
        viewModel.registerResult.observe(this, Observer { result ->
            if (result.isSuccess) {
                ToastUtils.showToast(this, "Registrasi berhasil, silakan login.", position = ToastUtils.Position.TOP)
                startActivity(Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                })
                finish()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            } else {
                ToastUtils.showToast(this,"Registrasi gagal: ${result.exceptionOrNull()?.message}", position = ToastUtils.Position.TOP)
            }
        })

        viewModel.loading.observe(this, Observer { isLoading ->
            binding.registerBtn.isEnabled = !isLoading
        })
    }

    private fun isAllFieldsFilled(): Boolean {
        return binding.emailField.text.toString().isNotEmpty() &&
                binding.passwordField.text.toString().isNotEmpty() &&
                binding.confirmPasswordField.text.toString().isNotEmpty() && binding.nameField.text.toString().isNotEmpty() &&
                binding.ageField.text.toString().isNotEmpty() && binding.actvGender.text.toString().isNotEmpty()
    }

    private fun setButtonState() {
        btnState = isAllFieldsFilled()
        binding.registerBtn.isEnabled = btnState

        if (btnState) {
            binding.registerBtn.setBackgroundColor(
                ContextCompat.getColor(this,R.color.bluePrimary)
            )
        } else {
            binding.registerBtn.setBackgroundColor(
                ContextCompat.getColor(this, R.color.gray400)
            )
        }
    }


    private fun setupTextWatchers() {
        binding.emailField.addTextChangedListener { setButtonState() }
        binding.passwordField.addTextChangedListener { setButtonState() }
        binding.confirmPasswordField.addTextChangedListener { setButtonState() }
    }



    // Fungsi untuk menampilkan dialog konfirmasi saat kembali
    private fun showBackConfirmationDialog() {
        AlertDialog.Builder(this).apply {
            setTitle("Konfirmasi")
            setMessage("Apakah Anda yakin ingin kembali? Data yang sudah diisi akan hilang.")
            setPositiveButton("Ya") { _: DialogInterface, _: Int ->
                val intent = Intent(this@RegisterActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                finish()
            }
            setNegativeButton("Tidak") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            create().show()
        }
    }

    /** 🔹 Validasi Password dengan aturan yang ketat */
    private fun isValidPassword(password: String): Boolean {
        val passwordPattern = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&+=!]).{8,}$"
        return password.matches(passwordPattern.toRegex())
    }

//    override fun onBackPressed() {
//
//        showBackConfirmationDialog()
//
//    }



    override fun onDestroy() {
        super.onDestroy()
        _binding = null  // Menghapus binding untuk menghindari memory leak
    }
}
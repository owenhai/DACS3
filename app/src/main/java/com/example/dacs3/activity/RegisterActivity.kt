package com.example.dacs3.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dacs3.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        binding.registerBtn.setOnClickListener {
            registerUser()
        }

        binding.loginTxt.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun registerUser() {
        val username = binding.usernameEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
        val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

        // Validation
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        // Register with Firebase Auth
        Log.d("RegisterActivity", "Registering user: $email")
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("RegisterActivity", "Auth successful, generating custom ID...")
                    val firebaseUid = firebaseAuth.currentUser?.uid

                    // Generate custom ID: MV1, MV2, MV3, ...
                        database.getReference("Users").get()
                            .addOnSuccessListener { snapshot ->
                                val userCount = snapshot.childrenCount.toInt() + 1
                                val customUserId = "MV$userCount"
                                Log.d("RegisterActivity", "Generated custom ID: $customUserId")

                                firebaseUid?.let {
                                    // Save user info with custom ID
                                    val userMap = mapOf(
                                        "customUserId" to customUserId,
                                        "firebaseUid" to firebaseUid,
                                        "username" to username,
                                        "email" to email,
                                        "password" to password,  // ⚠️ Lưu password (không an toàn)
                                        "permission" to "user",
                                        "createdAt" to System.currentTimeMillis()
                                    )

                                database.getReference("Users").child(customUserId).setValue(userMap)
                                    .addOnCompleteListener { dbTask ->
                                        if (dbTask.isSuccessful) {
                                            Log.d("RegisterActivity", "User saved successfully: $customUserId")
                                            // Save to SharedPreferences
                                            val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                                            sharedPref.edit().apply {
                                                putString("username", username)
                                                putString("email", email)
                                                putString("customUserId", customUserId)
                                                putString("firebaseUid", firebaseUid)
                                                apply()
                                            }

                                            Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                                            startActivity(Intent(this, MainActivity::class.java))
                                            finish()
                                        } else {
                                            Log.e("RegisterActivity", "Error: ${dbTask.exception?.message}")
                                            Toast.makeText(this, "Error saving user data: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                        }
                } else {
                    Log.e("RegisterActivity", "Auth failed: ${task.exception?.message}")
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}


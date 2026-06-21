package com.example.dacs3.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dacs3.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Check if user already logged in
        checkUserSession()

        binding.loginBtn.setOnClickListener {
            loginUser()
        }

        binding.registerTxt.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        binding.forgotPasswordTxt.setOnClickListener {
            resetPassword()
        }
    }

    private fun loginUser() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("LoginActivity", "Attempting login with email: $email")

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("LoginActivity", "Firebase Auth successful")
                    // Get username from database - search by email
                    val firebaseUid = firebaseAuth.currentUser?.uid
                    Log.d("LoginActivity", "Firebase UID: $firebaseUid")

                    firebaseUid?.let {
                        Log.d("LoginActivity", "Querying Users database...")
                        database.getReference("Users").orderByChild("firebaseUid").equalTo(firebaseUid).get()
                            .addOnSuccessListener { snapshot ->
                                Log.d("LoginActivity", "Query successful, exists: ${snapshot.exists()}, count: ${snapshot.childrenCount}")

                                if (snapshot.exists()) {
                                    // User data exists
                                    for (userSnapshot in snapshot.children) {
                                        val customUserId = userSnapshot.child("customUserId").value?.toString() ?: "User"
                                        val username = userSnapshot.child("username").value?.toString() ?: "User"
                                        val savedPassword = userSnapshot.child("password").value?.toString() ?: ""
                                        val permission = userSnapshot.child("permission").value?.toString() ?: "user"

                                        Log.d("LoginActivity", "Found user: $customUserId, $username, permission: $permission")

                                        // Save to SharedPreferences
                                        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                                        sharedPref.edit().apply {
                                            putString("username", username)
                                            putString("email", email)
                                            putString("customUserId", customUserId)
                                            putString("firebaseUid", firebaseUid)
                                            putString("password", savedPassword)
                                            putString("permission", permission)
                                            if (binding.rememberCheckBox.isChecked) {
                                                putBoolean("rememberMe", true)
                                            }
                                            apply()
                                        }

                                        Log.d("LoginActivity", "SharedPreferences saved, navigating to MainActivity...")
                                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, MainActivity::class.java))
                                        finish()
                                    }
                                } else {
                                    Log.d("LoginActivity", "User data not found in database, migrating...")
                                    // User data not found - migrate from Auth
                                    migrateUserFromAuth(firebaseUid, email)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("LoginActivity", "Query failed: ${e.message}")
                                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Log.e("LoginActivity", "Login failed: ${task.exception?.message}")
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun migrateUserFromAuth(firebaseUid: String, email: String) {
        // Tạo entry cho user cũ từ Firebase Authentication
        database.getReference("Users").get()
            .addOnSuccessListener { snapshot ->
                val userCount = snapshot.childrenCount.toInt() + 1
                val customUserId = "MV$userCount"

                val userMap = mapOf(
                    "customUserId" to customUserId,
                    "firebaseUid" to firebaseUid,
                    "username" to email.substringBefore("@"), // Username = email prefix
                    "email" to email,
                    "password" to "unknown",  // ⚠️ Không biết password cũ
                    "permission" to "user",
                    "createdAt" to System.currentTimeMillis()
                )

                database.getReference("Users").child(customUserId).setValue(userMap)
                    .addOnCompleteListener { dbTask ->
                        if (dbTask.isSuccessful) {
                            // Save to SharedPreferences
                            val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                            sharedPref.edit().apply {
                                putString("username", email.substringBefore("@"))
                                putString("email", email)
                                putString("customUserId", customUserId)
                                putString("firebaseUid", firebaseUid)
                                putString("password", "unknown")
                                putString("permission", "user")
                                if (binding.rememberCheckBox.isChecked) {
                                    putBoolean("rememberMe", true)
                                }
                                apply()
                            }

                            Toast.makeText(this, "Account migrated! Login successful", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Error: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
    }

    private fun resetPassword() {
        val email = binding.emailEditText.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            return
        }

        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Reset link sent to $email", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkUserSession() {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val rememberMe = sharedPref.getBoolean("rememberMe", false)

        if (rememberMe && firebaseAuth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}

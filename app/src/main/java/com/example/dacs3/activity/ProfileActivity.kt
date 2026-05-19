package com.example.dacs3.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.dacs3.R
import com.example.dacs3.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private var imageUri: Uri? = null
    private var currentCustomUserId: String? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            imageUri = result.data?.data
            binding.profileImg.setImageURI(imageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        loadUserData()

        binding.backBtn.setOnClickListener { finish() }

        binding.editImageBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImage.launch(intent)
        }

        binding.saveBtn.setOnClickListener {
            updateProfile()
        }

        binding.logoutBtn.setOnClickListener {
            logoutUser()
        }
    }

    private fun logoutUser() {
        auth.signOut()
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        sharedPref.edit().apply {
            clear()
            apply()
        }
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun loadUserData() {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        currentCustomUserId = sharedPref.getString("customUserId", null)

        if (currentCustomUserId == null) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        database.getReference("Users").child(currentCustomUserId!!).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val name = snapshot.child("username").value.toString()
                    val email = snapshot.child("email").value.toString()
                    val profileImg = snapshot.child("profileImage").value?.toString()

                    binding.displayNameEt.setText(name)
                    binding.emailEt.setText(email)
                    binding.usernameTv.text = name

                    if (!profileImg.isNullOrEmpty()) {
                        Glide.with(this@ProfileActivity)
                            .load(profileImg)
                            .placeholder(R.drawable.profile)
                            .into(binding.profileImg)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateProfile() {
        val newName = binding.displayNameEt.text.toString().trim()
        val newEmail = binding.emailEt.text.toString().trim()

        if (newName.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        binding.saveBtn.isEnabled = false
        binding.saveBtn.text = "Updating..."

        if (imageUri != null) {
            uploadImage(newName, newEmail)
        } else {
            saveToDatabase(newName, newEmail, null)
        }
    }

    private fun uploadImage(name: String, email: String) {
        if (imageUri == null) return

        try {
            val inputStream = contentResolver.openInputStream(imageUri!!)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)

            // Compress image to Base64 string
            val outputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 50, outputStream)
            val byteArray = outputStream.toByteArray()
            val base64Image = android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)

            // Save to database as a string
            saveToDatabase(name, email, "data:image/jpeg;base64,$base64Image")
        } catch (e: Exception) {
            binding.saveBtn.isEnabled = true
            binding.saveBtn.text = "Save Changes"
            Toast.makeText(this, "Image processing failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveToDatabase(name: String, email: String, imageUrl: String?) {
        val updates = mutableMapOf<String, Any>(
            "username" to name,
            "email" to email
        )
        if (imageUrl != null) {
            updates["profileImage"] = imageUrl
        }

        database.getReference("Users").child(currentCustomUserId!!).updateChildren(updates)
            .addOnSuccessListener {
                // Update SharedPreferences
                val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                sharedPref.edit().apply {
                    putString("username", name)
                    putString("email", email)
                    apply()
                }

                // If email changed, we might need to update Firebase Auth email as well
                val user = auth.currentUser
                if (user != null && user.email != email) {
                    user.updateEmail(email).addOnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            Toast.makeText(this, "Auth email update failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                binding.saveBtn.isEnabled = true
                binding.saveBtn.text = "Save Changes"
                Toast.makeText(this, "Update failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

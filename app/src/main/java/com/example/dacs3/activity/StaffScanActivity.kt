package com.example.dacs3.activity

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.provider.MediaStore
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.dacs3.databinding.ActivityStaffScanBinding
import com.google.firebase.database.FirebaseDatabase
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions

class StaffScanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStaffScanBinding
    private val database = FirebaseDatabase.getInstance()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    val contents = scanQRFromBitmap(bitmap)
                    if (contents != null) {
                        handleScanResult(contents)
                    } else {
                        Toast.makeText(this, "Could not find QR code in image", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun scanQRFromBitmap(bitmap: android.graphics.Bitmap): String? {
        val intArray = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source: LuminanceSource = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
        val bBitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = MultiFormatReader()
        return try {
            val result = reader.decode(bBitmap)
            result.text
        } catch (e: Exception) {
            null
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startScanner()
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_SHORT).show()
            }
        }

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents == null) {
            android.util.Log.d("StaffScan", "Scanner closed or failed")
            Toast.makeText(this, "Try Manual Verify if camera fails", Toast.LENGTH_SHORT).show()
        } else {
            handleScanResult(result.contents)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityStaffScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backBtn.setOnClickListener { finish() }
        binding.startScanBtn.setOnClickListener {
            checkPermissionAndStart()
        }

        binding.manualVerifyBtn.setOnClickListener {
            val ticketId = binding.ticketIdEt.text.toString().trim()
            if (ticketId.isNotEmpty()) {
                verifyTicket(ticketId)
            } else {
                Toast.makeText(this, "Please enter ticket ID", Toast.LENGTH_SHORT).show()
            }
        }

        binding.pickGalleryBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }
    }

    private fun checkPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                startScanner()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startScanner() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scan a ticket QR code")
        options.setCameraId(0)
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(true)
        options.setOrientationLocked(false)
        options.setCaptureActivity(com.journeyapps.barcodescanner.CaptureActivity::class.java) // Đảm bảo gọi Activity mặc định của thư viện
        barcodeLauncher.launch(options)
    }

    private fun handleScanResult(content: String) {
        val trimmedContent = content.trim()
        android.util.Log.d("StaffScan", "Scanned content: $trimmedContent")

        when {
            trimmedContent.startsWith("TICKET_VALIDATION:") -> {
                val ticketId = trimmedContent.substringAfter("TICKET_VALIDATION:")
                verifyTicket(ticketId)
            }
            trimmedContent.startsWith("TKT") -> {
                verifyTicket(trimmedContent)
            }
            trimmedContent.startsWith("PAYMENT_FOR_TICKET_") -> {
                // Người dùng quét nhầm mã QR thanh toán (Payment QR)
                val ticketId = trimmedContent.substringAfter("PAYMENT_FOR_TICKET_").substringBefore("_AMOUNT_")
                binding.resultCard.visibility = android.view.View.VISIBLE
                binding.resultStatusTxt.text = "WRONG QR CODE"
                binding.resultStatusTxt.setTextColor(android.graphics.Color.RED)
                binding.resultDetailsTxt.text = "This is a PAYMENT QR. Please pay first and scan the TICKET QR shown after payment.\nTicket ID: $ticketId"
            }
            else -> {
                // Thử verify trực tiếp nếu là chuỗi lạ
                verifyTicket(trimmedContent)
            }
        }
    }

    private fun verifyTicket(ticketId: String) {
        android.util.Log.d("StaffScan", "Starting verification for ID: $ticketId")

        // Hiển thị trạng thái đang kiểm tra
        binding.resultCard.visibility = android.view.View.VISIBLE
        binding.resultStatusTxt.text = "CHECKING..."
        binding.resultStatusTxt.setTextColor(android.graphics.Color.WHITE)
        binding.resultDetailsTxt.text = "Please wait while we verify the ticket: $ticketId"

        database.getReference("Tickets").child(ticketId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val status = snapshot.child("status").value.toString()
                val movie = snapshot.child("movieTitle").value.toString()
                val seats = snapshot.child("seatNo").value.toString()

                android.util.Log.d("StaffScan", "Ticket found! Status: $status")

                if (status == "Paid") {
                    // Valid ticket, mark as Used
                    database.getReference("Tickets").child(ticketId).child("status").setValue("Used")
                        .addOnSuccessListener {
                            binding.resultStatusTxt.text = "SUCCESS: TICKET VALID"
                            binding.resultDetailsTxt.text = "Movie: $movie\nSeats: $seats\nStatus: Used"
                            binding.resultStatusTxt.setTextColor(android.graphics.Color.GREEN)
                        }
                } else if (status == "Used") {
                    binding.resultStatusTxt.text = "ALREADY USED"
                    binding.resultDetailsTxt.text = "This ticket was already scanned.\nMovie: $movie\nSeats: $seats"
                    binding.resultStatusTxt.setTextColor(android.graphics.Color.YELLOW)
                } else {
                    binding.resultStatusTxt.text = "INVALID STATUS: $status"
                    binding.resultDetailsTxt.text = "Ticket found but not paid yet.\nMovie: $movie"
                    binding.resultStatusTxt.setTextColor(android.graphics.Color.RED)
                }
            } else {
                android.util.Log.d("StaffScan", "Ticket ID not found in DB: $ticketId")
                binding.resultStatusTxt.text = "NOT FOUND"
                binding.resultDetailsTxt.text = "Ticket ID $ticketId does not exist in our system."
                binding.resultStatusTxt.setTextColor(android.graphics.Color.RED)
            }
        }.addOnFailureListener {
            android.util.Log.e("StaffScan", "Database Error: ${it.message}")
            binding.resultStatusTxt.text = "SERVER ERROR"
            binding.resultDetailsTxt.text = "Error: ${it.message}"
            binding.resultStatusTxt.setTextColor(android.graphics.Color.RED)
        }
    }
}

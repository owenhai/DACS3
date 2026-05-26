package com.example.dacs3.activity

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.provider.MediaStore
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.dacs3.databinding.ActivityStaffScanBinding
import com.google.firebase.database.FirebaseDatabase
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.MultiFormatWriter
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.WriterException
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.BarcodeEncoder
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
        options.setCameraId(0) // Use back camera
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(true)
        options.setOrientationLocked(false) // Allow library to handle orientation automatically
        // REMOVED: options.setCaptureActivity(...) - Use default behavior
        barcodeLauncher.launch(options)
    }

    private fun handleScanResult(content: String) {
        val trimmedContent = content.trim()
        android.util.Log.d("StaffScan", "Scanned content: '$trimmedContent'")

        // Hide result components initially
        binding.resultCard.visibility = android.view.View.GONE
        binding.confirmCheckInBtn.visibility = android.view.View.GONE
        binding.postCheckInActions.visibility = android.view.View.GONE
        binding.moviePosterImg.visibility = android.view.View.GONE

        // Regex to find TICKET_VALIDATION pattern even if embedded in other text
        // TICKET_VALIDATION|TKT1716300000|Movie Title|Date|Time|Seats|Price
        val ticketRegex = Regex("TICKET_VALIDATION\\|([^|\\n\\r]+)\\|([^|\\n\\r]+)\\|([^|\\n\\r]+)\\|([^|\\n\\r]+)\\|([^|\\n\\r]+)\\|([^|\\n\\r]+)")
        val matchResult = ticketRegex.find(trimmedContent)

        if (matchResult != null) {
            val (ticketId, title, date, time, seats, price) = matchResult.destructured

            val cleanId = ticketId.trim()
            android.util.Log.d("StaffScan", "Parsed via Regex: ID='$cleanId', Title='$title'")

            binding.resultCard.visibility = android.view.View.VISIBLE
            binding.resultStatusTxt.text = "TICKET DETECTED"
            binding.resultStatusTxt.setTextColor(android.graphics.Color.CYAN)

            val details = "Movie: $title\nDate: $date\nTime: $time\nSeats: $seats"
            binding.resultDetailsTxt.text = details

            val summary = "TOTAL AMOUNT: $price VND\n--------------------------\nScanning database for status..."
            binding.billSummaryTxt.text = summary

            verifyTicket(cleanId)
            return
        }

        // Fallback for older formats or plain IDs
        when {
            trimmedContent.contains("TICKET_VALIDATION:") -> {
                val ticketId = trimmedContent.substringAfter("TICKET_VALIDATION:").split("|")[0].trim()
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
        if (ticketId.isEmpty()) {
            Toast.makeText(this, "Empty ticket ID", Toast.LENGTH_SHORT).show()
            return
        }

        android.util.Log.d("StaffScan", "Starting verification for ID: $ticketId")

        // Show checking status in UI if resultCard is not already visible
        if (binding.resultCard.visibility != android.view.View.VISIBLE) {
            binding.resultCard.visibility = android.view.View.VISIBLE
            binding.resultStatusTxt.text = "CHECKING..."
            binding.resultStatusTxt.setTextColor(android.graphics.Color.WHITE)
            binding.resultDetailsTxt.text = "Verifying ticket: $ticketId"
        }
        binding.confirmCheckInBtn.visibility = android.view.View.GONE
        binding.postCheckInActions.visibility = android.view.View.GONE

        database.getReference("Tickets").child(ticketId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val status = snapshot.child("status").value.toString()
                val movie = snapshot.child("movieTitle").value.toString()
                val seats = snapshot.child("seatNo").value.toString()
                val date = snapshot.child("showDate").value.toString()
                val time = snapshot.child("showTime").value.toString()
                val price = snapshot.child("totalPrice").value?.toString() ?: "0"

                android.util.Log.d("StaffScan", "Ticket found! Status: $status")

                // Try to find and show movie poster
                fetchMoviePoster(movie)

                val summaryBuilder = StringBuilder()
                summaryBuilder.append("TICKET ID: $ticketId\n")
                summaryBuilder.append("PRICE: $price VND\n")
                summaryBuilder.append("STATUS: ${status.uppercase()}\n")
                summaryBuilder.append("--------------------------\n")

                if (status == "Paid") {
                    summaryBuilder.append("READY FOR ADMISSION")
                    // Valid ticket, wait for staff to confirm use
                    binding.resultStatusTxt.text = "VALID TICKET: READY"
                    binding.resultStatusTxt.setTextColor(android.graphics.Color.GREEN)

                    binding.confirmCheckInBtn.visibility = android.view.View.VISIBLE
                    binding.confirmCheckInBtn.setOnClickListener {
                        updateTicketStatusToUsed(ticketId, movie, date, time, seats, price)
                    }
                } else if (status == "Used") {
                    summaryBuilder.append("ALREADY SCANNED AT THEATER")
                    binding.resultStatusTxt.text = "ALREADY USED"
                    binding.resultStatusTxt.setTextColor(android.graphics.Color.YELLOW)
                    
                    // Allow re-printing/emailing even if already used
                    showPostCheckInActions(ticketId, movie, date, time, seats, price)
                } else {
                    summaryBuilder.append("ACTION REQUIRED: CHECK PAYMENT")
                    binding.resultStatusTxt.text = "NOT PAID / EXPIRED"
                    binding.resultStatusTxt.setTextColor(android.graphics.Color.RED)
                }

                binding.resultDetailsTxt.text = "Movie: $movie\nDate: $date\nTime: $time\nSeats: $seats"
                binding.billSummaryTxt.text = summaryBuilder.toString()

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

    private fun updateTicketStatusToUsed(ticketId: String, movie: String, date: String, time: String, seats: String, price: String) {
        binding.confirmCheckInBtn.isEnabled = false
        database.getReference("Tickets").child(ticketId).child("status").setValue("Used")
            .addOnSuccessListener {
                binding.resultStatusTxt.text = "CHECK-IN SUCCESSFUL"
                binding.resultStatusTxt.setTextColor(android.graphics.Color.GREEN)
                binding.confirmCheckInBtn.visibility = android.view.View.GONE
                
                showPostCheckInActions(ticketId, movie, date, time, seats, price)
                Toast.makeText(this, "Ticket marked as USED", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                binding.confirmCheckInBtn.isEnabled = true
                Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showPostCheckInActions(ticketId: String, movie: String, date: String, time: String, seats: String, price: String) {
        binding.postCheckInActions.visibility = android.view.View.VISIBLE
        
        binding.printTicketBtn.setOnClickListener {
            generateCgvStyleBill(ticketId, movie, date, time, seats, price)
        }
        
        // Removed sendEmailBtn listener as it's no longer in the layout
    }

    private fun generateCgvStyleBill(ticketId: String, movie: String, date: String, time: String, seats: String, price: String) {
        try {
            val pdfFile = java.io.File(cacheDir, "CGV_Bill_$ticketId.pdf")
            val outputStream = java.io.FileOutputStream(pdfFile)

            val pdfDocument = android.graphics.pdf.PdfDocument()
            // Narrower page like a thermal receipt (e.g., 80mm width ~ 226 points)
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(250, 650, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = android.graphics.Paint()
            val centerX = 125f

            // --- HEADER ---
            paint.textAlign = android.graphics.Paint.Align.CENTER
            paint.isFakeBoldText = true
            paint.textSize = 20f
            canvas.drawText("CGV CINEMAS", centerX, 50f, paint)

            paint.textSize = 10f
            paint.isFakeBoldText = false
            canvas.drawText("Cultureplex - Vietnam", centerX, 70f, paint)
            canvas.drawText("------------------------------------------", centerX, 85f, paint)

            // --- MOVIE TITLE ---
            paint.textSize = 14f
            paint.isFakeBoldText = true
            // Wrap text if movie title is too long
            val movieTitle = movie.uppercase()
            if (movieTitle.length > 20) {
                canvas.drawText(movieTitle.substring(0, 20), centerX, 115f, paint)
                canvas.drawText(movieTitle.substring(20), centerX, 135f, paint)
            } else {
                canvas.drawText(movieTitle, centerX, 115f, paint)
            }

            // --- DETAILS ---
            paint.textAlign = android.graphics.Paint.Align.LEFT
            paint.textSize = 11f
            paint.isFakeBoldText = false
            var y = 170f
            canvas.drawText("DATE: $date", 30f, y, paint); y += 25f
            canvas.drawText("TIME: $time", 30f, y, paint); y += 25f
            canvas.drawText("ROOM: CINEMA 04 (2D)", 30f, y, paint); y += 25f

            paint.textSize = 13f
            paint.isFakeBoldText = true
            canvas.drawText("SEATS: $seats", 30f, y, paint); y += 35f

            paint.textSize = 11f
            paint.isFakeBoldText = false
            canvas.drawText("PRICE: $price VND", 30f, y, paint); y += 20f
            canvas.drawText("------------------------------------------", 20f, y, paint); y += 25f

            // --- FOOTER & BARCODE ---
            paint.textAlign = android.graphics.Paint.Align.CENTER
            canvas.drawText("TICKET ID: $ticketId", centerX, y, paint); y += 30f

            // Replace black rectangle with actual Ticket QR Code
            val qrBitmap = generateTicketQrBitmap(ticketId, movie, date, time, seats, price)
            if (qrBitmap != null) {
                // Center the QR code (QR is 150x150)
                val qrX = centerX - 75f
                canvas.drawBitmap(qrBitmap, qrX, y, paint)
                y += 160f
            } else {
                // Fallback to black rect if QR fails
                paint.color = android.graphics.Color.BLACK
                canvas.drawRect(50f, y, 200f, y + 40f, paint)
                y += 60f
            }

            paint.textSize = 9f
            canvas.drawText("Enjoy your movie!", centerX, y, paint)

            pdfDocument.finishPage(page)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            // Open the PDF for printing/viewing
            val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.fileprovider", pdfFile)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(intent, "Open Bill (In vé)"))

        } catch (e: Exception) {
            android.util.Log.e("StaffScan", "Error creating CGV Bill: ${e.message}")
            Toast.makeText(this, "Failed to generate bill", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateTicketQrBitmap(ticketId: String, movie: String, date: String, time: String, seats: String, price: String): Bitmap? {
        val content = "TICKET_VALIDATION|$ticketId|$movie|$date|$time|$seats|$price"
        val multiFormatWriter = MultiFormatWriter()
        return try {
            val hints = mutableMapOf<com.google.zxing.EncodeHintType, Any>()
            hints[com.google.zxing.EncodeHintType.ERROR_CORRECTION] = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
            hints[com.google.zxing.EncodeHintType.MARGIN] = 1
            hints[com.google.zxing.EncodeHintType.CHARACTER_SET] = "UTF-8"

            val bitMatrix = multiFormatWriter.encode(content, BarcodeFormat.QR_CODE, 150, 150, hints)
            val barcodeEncoder = BarcodeEncoder()
            barcodeEncoder.createBitmap(bitMatrix)
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }

    private fun printTicket(ticketId: String, movie: String, date: String, time: String, seats: String, price: String) {
        val ticketInfo = "TICKET ID: $ticketId\nMOVIE: $movie\nDATE: $date\nTIME: $time\nSEATS: $seats\nPRICE: $$price"
        
        val printManager = getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
        val jobName = "${getString(com.example.dacs3.R.string.app_name)} Ticket $ticketId"
        
        // Simple implementation using a custom print adapter for text
        // In a real app, this would generate a PDF or use a formatted HTML string
        Toast.makeText(this, "Preparing ticket for printing...", Toast.LENGTH_SHORT).show()
        
        // For demonstration, we share it as a text file which is a common way to "print" or save on mobile
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, jobName)
        intent.putExtra(Intent.EXTRA_TEXT, ticketInfo)
        startActivity(Intent.createChooser(intent, "Print/Save Ticket"))
    }

    private fun sendTicketEmail(ticketId: String, movie: String, date: String, time: String, seats: String, price: String) {
        try {
            // Create a PDF file in the app's cache directory
            val pdfFile = java.io.File(cacheDir, "Ticket_$ticketId.pdf")
            val outputStream = java.io.FileOutputStream(pdfFile)

            val pdfDocument = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(300, 500, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = android.graphics.Paint()

            // Draw ticket content
            paint.textSize = 14f
            paint.isFakeBoldText = true
            canvas.drawText("MOVIE TICKET", 80f, 40f, paint)

            paint.isFakeBoldText = false
            paint.textSize = 10f
            var yPos = 80f
            canvas.drawText("Ticket ID: $ticketId", 20f, yPos, paint); yPos += 30f
            canvas.drawText("Movie: $movie", 20f, yPos, paint); yPos += 30f
            canvas.drawText("Date: $date", 20f, yPos, paint); yPos += 30f
            canvas.drawText("Time: $time", 20f, yPos, paint); yPos += 30f
            canvas.drawText("Seats: $seats", 20f, yPos, paint); yPos += 30f
            canvas.drawText("Price: $$price", 20f, yPos, paint); yPos += 40f

            paint.textSize = 8f
            canvas.drawText("Thank you for choosing our cinema!", 50f, yPos, paint)

            pdfDocument.finishPage(page)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            // Get URI using FileProvider for security
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "application/pdf"
            intent.putExtra(Intent.EXTRA_SUBJECT, "Your Movie Ticket: $movie")
            intent.putExtra(Intent.EXTRA_TEXT, "Please find your movie ticket attached as PDF.")
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            startActivity(Intent.createChooser(intent, "Send Ticket PDF"))
        } catch (e: Exception) {
            android.util.Log.e("StaffScan", "Error creating PDF: ${e.message}")
            Toast.makeText(this, "Failed to create ticket PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchMoviePoster(movieTitle: String) {
        // Search in Items and Upcomming
        database.getReference("Items").orderByChild("Title").equalTo(movieTitle).limitToFirst(1)
            .get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val film = snapshot.children.first().getValue(com.example.dacs3.model.Film::class.java)
                    film?.Poster?.let { showImage(it) }
                } else {
                    database.getReference("Upcomming").orderByChild("Title").equalTo(movieTitle).limitToFirst(1)
                        .get().addOnSuccessListener { upcomingSnapshot ->
                            if (upcomingSnapshot.exists()) {
                                val film = upcomingSnapshot.children.first().getValue(com.example.dacs3.model.Film::class.java)
                                film?.Poster?.let { showImage(it) }
                            }
                        }
                }
            }
    }

    private fun showImage(url: String) {
        binding.moviePosterImg.visibility = android.view.View.VISIBLE
        Glide.with(this)
            .load(url)
            .into(binding.moviePosterImg)
    }
}

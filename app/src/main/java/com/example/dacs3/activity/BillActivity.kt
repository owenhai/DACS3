package com.example.dacs3.activity

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.dacs3.R
import com.bumptech.glide.Glide
import com.example.dacs3.databinding.ActivityBillBinding
import com.example.dacs3.model.Ticket
import com.google.firebase.database.FirebaseDatabase
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder

class BillActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBillBinding
    private lateinit var ticket: Ticket
    private val database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBillBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val ticketExtra = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("ticket", Ticket::class.java)
        } else {
            intent.getSerializableExtra("ticket") as? Ticket
        }

        if (ticketExtra == null) {
            Toast.makeText(this, "Error: Ticket data not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        ticket = ticketExtra

        setupUI()
        generatePaymentQR()

        binding.backBtn.setOnClickListener { finish() }
        binding.confirmPaymentBtn.setOnClickListener {
            simulatePayment()
        }
    }

    private fun setupUI() {
        binding.movieTitleTxt.text = ticket.movieTitle
        binding.dateTimeTxt.text = "${ticket.showDate} | ${ticket.showTime}"
        binding.seatsTxt.text = "Seats: ${ticket.seatNo}"
        binding.totalPriceTxt.text = "Total: ${formatVnd(ticket.totalPrice)}"
        
        // Show food details if any
        if (ticket.foodDetails.isNotEmpty()) {
            binding.seatsTxt.text = "${binding.seatsTxt.text}\nSnacks: ${ticket.foodDetails}"
        }

        binding.ticketIdDisplayTxt.text = "ID: ${ticket.ticketId}"
        updateStatusUI()
    }

    private fun formatVnd(amount: Double): String {
        val formatter = java.text.DecimalFormat("#,###")
        val symbols = java.text.DecimalFormatSymbols(java.util.Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        formatter.decimalFormatSymbols = symbols
        return "${formatter.format(amount)} đ"
    }

    private fun updateStatusUI() {
        binding.statusTxt.text = "Status: ${ticket.status}"
        when (ticket.status) {
            "Pending" -> {
                binding.statusTxt.setBackgroundResource(R.drawable.yellow_bg)
                binding.qrHintTxt.text = "Scan to Pay via VietQR"
                binding.confirmPaymentBtn.visibility = View.VISIBLE
            }
            "Paid" -> {
                binding.statusTxt.setBackgroundResource(R.drawable.green_bg)
                binding.statusTxt.setTextColor(Color.WHITE)
                binding.qrHintTxt.text = "Show this QR at the cinema"
                binding.confirmPaymentBtn.visibility = View.GONE
                generateTicketQR()
            }
            "Used" -> {
                binding.statusTxt.setBackgroundResource(R.drawable.light_black_bg)
                binding.statusTxt.setTextColor(Color.GRAY)
                binding.qrHintTxt.text = "Ticket has been used"
                binding.confirmPaymentBtn.visibility = View.GONE
                binding.qrCodeImg.alpha = 0.5f
            }
        }
    }

    private fun generatePaymentQR() {
        // Construct VietQR Quick Link URL
        val bankId = "970422"
        val accountNo = "7705092006"
        val template = "compact"
        val amount = ticket.totalPrice.toInt().toString()
        val description = ticket.ticketId
        val accountName = "TRAN LE QUOC ANH"

        val vietQrUrl = "https://img.vietqr.io/image/$bankId-$accountNo-$template.png" +
                "?amount=$amount" +
                "&addInfo=$description" +
                "&accountName=${java.net.URLEncoder.encode(accountName, "UTF-8")}"

        // Load the VietQR image using Glide
        Glide.with(this)
            .load(vietQrUrl)
            .placeholder(R.drawable.blur_bg)
            .into(binding.qrCodeImg)
    }

    private fun generateTicketQR() {
        // Structured content for scannability and quick info display
        // Format: TICKET_VALIDATION|ID|TITLE|DATE|TIME|SEATS|PRICE
        val content = "TICKET_VALIDATION|" +
                "${ticket.ticketId}|" +
                "${ticket.movieTitle}|" +
                "${ticket.showDate}|" +
                "${ticket.showTime}|" +
                "${ticket.seatNo}|" +
                "${formatVnd(ticket.totalPrice)}"

        generateQRCode(content)
    }

    private fun generateQRCode(content: String) {
        val multiFormatWriter = MultiFormatWriter()
        try {
            val hints = mutableMapOf<com.google.zxing.EncodeHintType, Any>()
            hints[com.google.zxing.EncodeHintType.ERROR_CORRECTION] = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
            hints[com.google.zxing.EncodeHintType.MARGIN] = 2
            hints[com.google.zxing.EncodeHintType.CHARACTER_SET] = "UTF-8"

            val bitMatrix = multiFormatWriter.encode(content, BarcodeFormat.QR_CODE, 800, 800, hints)
            val barcodeEncoder = BarcodeEncoder()
            val bitmap: Bitmap = barcodeEncoder.createBitmap(bitMatrix)
            binding.qrCodeImg.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }

    private fun simulatePayment() {
        // In a real app, this would be an SDK callback or a Firebase listener for status change
        database.getReference("Tickets").child(ticket.ticketId).child("status").setValue("Paid")
            .addOnSuccessListener {
                ticket.status = "Paid"
                markSeatsAsOccupied()
                updateStatusUI()
                // Regenerate the QR code now that it is Paid
                generateTicketQR()
                Toast.makeText(this, "Payment Successful!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Payment failed to record", Toast.LENGTH_SHORT).show()
            }
    }

    private fun markSeatsAsOccupied() {
        val movieTitle = ticket.movieTitle
        val date = ticket.showDate.replace("/", "_") // Firebase paths can't contain '/'
        val time = ticket.showTime.replace(":", "_").replace(" ", "_")
        val seats = ticket.seatNo.split(", ")

        val occupiedSeatsRef = database.getReference("OccupiedSeats")
            .child(movieTitle)
            .child(date)
            .child(time)

        val seatMap = mutableMapOf<String, Any>()
        for (seat in seats) {
            seatMap[seat] = true
        }

        occupiedSeatsRef.updateChildren(seatMap)
    }
}

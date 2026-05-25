package com.example.dacs3.activity

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.dacs3.R
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

        ticket = intent.getSerializableExtra("ticket") as Ticket

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
        binding.totalPriceTxt.text = "Total: $${String.format("%.2f", ticket.totalPrice)}"
        binding.ticketIdDisplayTxt.text = "ID: ${ticket.ticketId}"
        updateStatusUI()
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
        // Simplified VietQR simulation: https://img.vietqr.io/image/<BANK_ID>-<ACCOUNT_NO>-<TEMPLATE>.png
        // In a real app, you'd use the actual API. Here we generate a QR for the Bill ID.
        val paymentContent = "PAYMENT_FOR_TICKET_${ticket.ticketId}_AMOUNT_${ticket.totalPrice}"
        generateQRCode(paymentContent)
    }

    private fun generateTicketQR() {
        // This QR contains the ticket ID for staff to scan and verify
        generateQRCode("TICKET_VALIDATION:${ticket.ticketId}")
    }

    private fun generateQRCode(content: String) {
        val multiFormatWriter = MultiFormatWriter()
        try {
            val bitMatrix = multiFormatWriter.encode(content, BarcodeFormat.QR_CODE, 512, 512)
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

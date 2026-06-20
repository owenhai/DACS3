package com.example.dacs3.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.dacs3.databinding.ActivityAdminDashboardBinding

class AdminDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backBtn.setOnClickListener { finish() }

        binding.manageMoviesBtn.setOnClickListener {
            startActivity(Intent(this, AdminFilmsActivity::class.java))
        }

        binding.manageScheduleBtn.setOnClickListener {
            startActivity(Intent(this, AdminSchedulesActivity::class.java))
        }

        binding.manageTicketsBtn.setOnClickListener {
            startActivity(Intent(this, AdminTicketsActivity::class.java))
        }

        binding.staffScanBtn.setOnClickListener {
            startActivity(Intent(this, StaffScanActivity::class.java))
        }

        binding.manageRoomsBtn.setOnClickListener {
            startActivity(Intent(this, AdminRoomsActivity::class.java))
        }

        binding.revenueReportBtn.setOnClickListener {
            startActivity(Intent(this, AdminRevenueActivity::class.java))
        }
    }
}

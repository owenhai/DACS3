package com.example.dacs3.model

import java.io.Serializable

data class Ticket(
    val ticketId: String = "",
    val movieTitle: String = "",
    val showDate: String = "",
    val showTime: String = "",
    val seatNo: String = "",
    val sessionId: String = "", // Add this to track occupied seats
    val totalPrice: Double = 0.0,
    var isCheckedIn: Boolean = false,
    var status: String = "Pending", // "Pending", "Paid", "Used"
    var foodDetails: String = "",
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

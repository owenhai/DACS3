package com.example.dacs3.model

import java.io.Serializable

data class SeatConfig(
    val row: String = "",
    val type: String = "Standard",
    var price: Double = 0.0
) : Serializable


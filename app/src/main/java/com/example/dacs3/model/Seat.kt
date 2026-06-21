package com.example.dacs3.model

data class Seat(var status : SeatStatus, var name : String) {
    enum class SeatStatus{
        AVAILABLE,
        UNAVAILABLE,
        SELECTED
    }
}

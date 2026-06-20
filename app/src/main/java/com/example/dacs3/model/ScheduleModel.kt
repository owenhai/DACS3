package com.example.dacs3.model

import java.io.Serializable

data class ScheduleModel(
    var sessionId: String = "",
    var movieId: String = "",
    var movieTitle: String = "",
    var date: String = "",
    var roomName: String = "",
    var roomId: String = "",
    var timeSlots: List<String> = listOf(), // This will now contain exactly one formatted slot "Start - End"
    var timestamp: Long = 0
) : Serializable

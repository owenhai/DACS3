package com.example.dacs3.model

import java.io.Serializable

data class ShowDate(
    val id: String = "",
    var date: String = "",
    var timeSlots: List<String> = emptyList()
) : Serializable


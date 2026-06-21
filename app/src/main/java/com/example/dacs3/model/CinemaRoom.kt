package com.example.dacs3.model

import java.io.Serializable

data class CinemaRoom(
    var id: String = "",
    var name: String = "",
    var totalRows: Int = 9,
    var totalCols: Int = 9,
    var type: String = "Standard"
) : Serializable

package com.example.dacs3.model

import java.io.Serializable

data class Movie(
    val id: String = "",
    var title: String = "",
    var duration: Int = 0,
    var genre: String = ""
) : Serializable


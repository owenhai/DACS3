package com.example.dacs3.model

import java.io.Serializable

data class FoodItem(
    var id: String = "",
    var name: String = "",
    var price: Double = 0.0,
    var imagePath: String = "", // URL or resource name
    var description: String = ""
) : Serializable

package com.example.aplikacja_dla_strzelcow.data


import com.google.firebase.Timestamp

data class Series(
    val id: String = "",
    val weapon: String = "",
    val ammo: String = "",
    val distance: Int = 0,
    val createdAt: Timestamp? = null,
    val imageUrl: String? = null,
    val target: Map<String, Float>? = null
)

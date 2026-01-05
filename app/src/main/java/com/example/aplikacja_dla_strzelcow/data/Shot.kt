package com.example.aplikacja_dla_strzelcow.data

import com.google.firebase.Timestamp

data class Shot(
    val id: String = "",
    val x: Float = 0f,
    val y: Float = 0f,
    val value: Int = 0,
    val timestamp: Timestamp? = null
)
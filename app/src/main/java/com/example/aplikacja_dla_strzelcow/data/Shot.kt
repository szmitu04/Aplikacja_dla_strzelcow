package com.example.aplikacja_dla_strzelcow.data

import com.google.firebase.Timestamp
import java.util.UUID

data class Shot(
    val id: String = UUID.randomUUID().toString(),
    val x: Float = 0f,
    val y: Float = 0f,
    var value: Int = 0,
    val timestamp: Timestamp? = null
)
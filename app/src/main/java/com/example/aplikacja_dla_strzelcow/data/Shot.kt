package com.example.aplikacja_dla_strzelcow.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import java.util.UUID

data class Shot(
    val id: String = UUID.randomUUID().toString(),
    val x: Float = 0f,
    val y: Float = 0f,
    var value: Int = 0,
    val timestamp: Timestamp? = null,

    @get:PropertyName("manual") // Mówi Firestore: szukaj pola "manual" w bazie
    val isManual: Boolean = false,
    val userId: String = ""
)
package com.example.aplikacja_dla_strzelcow.data

import com.google.firebase.Timestamp

data class Session(
    val id: String = "",
    val createdAt: Timestamp? = null,
    val location: String = "",
    val notes: String = ""
)
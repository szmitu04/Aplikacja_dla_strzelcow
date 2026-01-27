package com.example.aplikacja_dla_strzelcow.data


import com.google.firebase.Timestamp

data class Series(
    val id: String = "",
    val weapon: String = "",
    val ammo: String = "",
    val distance: Int = 0,
    val notes: String = "",
    val createdAt: Timestamp? = null,
    val imageUrl: String? = null,
    val targetParams: TargetParams? = null,
    val userId: String = ""
)
data class TargetParams(
    val centerX: Float = 0f,
    val centerY: Float = 0f,
    val radius: Float = 0f
)
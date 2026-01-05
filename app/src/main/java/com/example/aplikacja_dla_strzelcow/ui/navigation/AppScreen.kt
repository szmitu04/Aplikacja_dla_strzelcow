package com.example.aplikacja_dla_strzelcow.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppScreen(
    val title: String,
    val icon: ImageVector
) {
    HOME("Home", Icons.Filled.Home),
    COMMUNITY("Społeczność", Icons.Filled.Group),
    PROFILE("Profil", Icons.Filled.Person)
}
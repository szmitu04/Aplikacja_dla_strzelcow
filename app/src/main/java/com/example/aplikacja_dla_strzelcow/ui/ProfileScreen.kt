package com.example.aplikacja_dla_strzelcow.ui


import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text("Profil użytkownika")

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onLogout) {
            Text("Wyloguj się")
        }
    }
}
package com.example.aplikacja_dla_strzelcow.ui


import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    isLoading: Boolean,
    onLoginClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text("Aplikacja dla strzelców")

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(onClick = onLoginClick) {
                    Text("Zaloguj się przez Google")
                }
            }
        }
    }
}
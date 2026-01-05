package com.example.aplikacja_dla_strzelcow


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aplikacja_dla_strzelcow.data.FirestoreRepository
import com.example.aplikacja_dla_strzelcow.ui.theme.Aplikacja_dla_strzelcowTheme

class AddShotActivity : ComponentActivity() {

    private val repository = FirestoreRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sessionId = intent.getStringExtra("sessionId") ?: return
        val seriesId = intent.getStringExtra("seriesId") ?: return

        setContent {
            var x by remember { mutableStateOf("") }
            var y by remember { mutableStateOf("") }
            var value by remember { mutableStateOf("") }

            Aplikacja_dla_strzelcowTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {

                    Text("Nowy strzał")

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = x,
                        onValueChange = { x = it },
                        label = { Text("x") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = y,
                        onValueChange = { y = it },
                        label = { Text("y") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("Punkty") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            repository.addShot(
                                sessionId = sessionId,
                                seriesId = seriesId,
                                x = x.toFloatOrNull() ?: 0f,
                                y = y.toFloatOrNull() ?: 0f,
                                value = value.toIntOrNull() ?: 0
                            )
                            finish()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Zapisz strzał")
                    }
                }
            }
        }
    }
}
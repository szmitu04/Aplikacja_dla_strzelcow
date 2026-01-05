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

class AddSessionActivity : ComponentActivity() {

    private val repository = FirestoreRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Aplikacja_dla_strzelcowTheme {


                var location by remember { mutableStateOf("") }
                var notes by remember { mutableStateOf("") }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {

                    Text("Nowy trening")

                    Spacer(modifier = Modifier.height(16.dp))



                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Miejsce") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notatki") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            repository.createSession(

                                location = location,
                                notes = notes
                            )
                            finish()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Zapisz trening")
                    }
                }
            }
        }
    }
}

package com.example.aplikacja_dla_strzelcow


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aplikacja_dla_strzelcow.data.FirestoreRepository
import com.example.aplikacja_dla_strzelcow.ui.theme.Aplikacja_dla_strzelcowTheme
import androidx.compose.material.icons.filled.CameraAlt
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import coil.compose.AsyncImage
import java.io.File
class AddSeriesActivity : ComponentActivity() {

    private val repository = FirestoreRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sessionId = intent.getStringExtra("sessionId") ?: return

        setContent {
            Aplikacja_dla_strzelcowTheme {
                val context = LocalContext.current
                var weapon by remember { mutableStateOf("") }
                var ammo by remember { mutableStateOf("") }
                var distance by remember { mutableStateOf("") }
                var notes by remember { mutableStateOf("") }
                var photoFile by remember { mutableStateOf<File?>(null) }
                var overlayPhotoFile by remember { mutableStateOf<File?>(null) }
                var originalPhotoFile by remember { mutableStateOf<File?>(null) }

                val cameraLauncher =
                    rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == RESULT_OK) {
                            val overlayPath = result.data?.getStringExtra("overlayPhotoPath")
                            if (overlayPath != null) {
                                overlayPhotoFile = File(overlayPath)
                            }
                            val originalPath = result.data?.getStringExtra("originalPhotoPath")
                            if (originalPath != null) {
                                originalPhotoFile = File(originalPath)
                            }
                        }
                    }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text("Nowa seria", style = MaterialTheme.typography.headlineSmall)

                    Spacer(modifier = Modifier.height(16.dp))

                    if (overlayPhotoFile == null) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "Aparat",
                            modifier = Modifier
                                .size(64.dp)
                                .clickable {
                                    cameraLauncher.launch(
                                        Intent(context, CameraActivity::class.java)
                                    )
                                }
                        )
                    } else {
                        AsyncImage(
                            model = overlayPhotoFile, // Wyświetlamy zdjęcie z otoczką
                            contentDescription = "Zdjęcie tarczy",
                            modifier = Modifier.size(200.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = weapon,
                        onValueChange = { weapon = it },
                        label = { Text("Broń") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ammo,
                        onValueChange = { ammo = it },
                        label = { Text("Amunicja") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = distance,
                        onValueChange = { distance = it },
                        label = { Text("Dystans (m)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Opis") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            repository.createSeries(
                                sessionId,
                                weapon,
                                ammo,
                                distance.toIntOrNull() ?: 0
                            ) { seriesId ->

                                // Używamy oryginalnego pliku do wysłania
                                originalPhotoFile?.let { file ->
                                    repository.uploadSeriesImage(
                                        sessionId,
                                        seriesId,
                                        file
                                    ) { url ->
                                        repository.updateSeriesImage(
                                            sessionId,
                                            seriesId,
                                            url
                                        )
                                    }
                                }

                                finish()
                            }
                        }
                    ) {
                        Text("Zapisz serię")
                    }
                }
            }
        }
    }
}
//
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.Button
//import androidx.compose.material3.OutlinedTextField
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import com.example.aplikacja_dla_strzelcow.data.FirestoreRepository
//import com.example.aplikacja_dla_strzelcow.ui.theme.Aplikacja_dla_strzelcowTheme
//
//class AddSeriesActivity : ComponentActivity() {
//
//    private val repository = FirestoreRepository()
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//
//        val sessionId = intent.getStringExtra("sessionId") ?: return
//
//        setContent {
//            var weapon by remember { mutableStateOf("") }
//            var ammo by remember { mutableStateOf("") }
//            var distance by remember { mutableStateOf("") }
//
//            Aplikacja_dla_strzelcowTheme {
//                Column(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .padding(16.dp),
//                    verticalArrangement = Arrangement.Center
//                ) {
//
//                    Text("Nowa seria")
//
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    OutlinedTextField(
//                        value = weapon,
//                        onValueChange = { weapon = it },
//                        label = { Text("Broń") },
//                        modifier = Modifier.fillMaxWidth()
//                    )
//
//                    Spacer(modifier = Modifier.height(8.dp))
//
//                    OutlinedTextField(
//                        value = ammo,
//                        onValueChange = { ammo = it },
//                        label = { Text("Amunicja") },
//                        modifier = Modifier.fillMaxWidth()
//                    )
//
//                    Spacer(modifier = Modifier.height(8.dp))
//
//                    OutlinedTextField(
//                        value = distance,
//                        onValueChange = { distance = it },
//                        label = { Text("Dystans (m)") },
//                        modifier = Modifier.fillMaxWidth()
//                    )
//
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    Button(
//                        onClick = {
//                            repository.createSeries(
//                                sessionId = sessionId,
//                                weapon = weapon,
//                                ammo = ammo,
//                                distance = distance.toIntOrNull() ?: 0
//                            )
//                            finish()
//                        },
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Text("Zapisz serię")
//                    }
//                }
//            }
//        }
//    }
//}
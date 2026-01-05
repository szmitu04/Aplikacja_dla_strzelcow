package com.example.aplikacja_dla_strzelcow


import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.aplikacja_dla_strzelcow.data.FirestoreRepository
import com.example.aplikacja_dla_strzelcow.data.Shot
import com.example.aplikacja_dla_strzelcow.ui.theme.Aplikacja_dla_strzelcowTheme

class ShotsActivity : ComponentActivity() {

    private val repository = FirestoreRepository()

    private lateinit var sessionId: String
    private lateinit var seriesId: String

    private var shotsState by mutableStateOf<List<Shot>>(emptyList())

    private fun loadShots() {
        repository.getShots(sessionId, seriesId) {
            shotsState = it
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sessionId = intent.getStringExtra("sessionId") ?: return
        seriesId = intent.getStringExtra("seriesId") ?: return

        setContent {
            val context = LocalContext.current
            val shots = shotsState

            Aplikacja_dla_strzelcowTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {

                    Text("Strzały w serii")

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(context, AddShotActivity::class.java).apply {
                                    putExtra("sessionId", sessionId)
                                    putExtra("seriesId", seriesId)
                                }
                            )
                        }
                    ) {
                        Text("Dodaj strzał")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn {
                        items(shots) { shot ->
                            ShotItem(shot)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadShots()
    }
}

//@Composable
//fun ShotItem(shot: Shot) {
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(12.dp)
//    ) {
//        Text("x = ${shot.x}, y = ${shot.y}")
//        Text("Punkty: ${shot.value}")
//    }
//}
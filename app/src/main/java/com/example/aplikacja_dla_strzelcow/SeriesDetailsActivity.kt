package com.example.aplikacja_dla_strzelcow


import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import com.example.aplikacja_dla_strzelcow.data.FirestoreRepository
import com.example.aplikacja_dla_strzelcow.data.Series
import com.example.aplikacja_dla_strzelcow.data.Shot
import com.example.aplikacja_dla_strzelcow.ui.theme.Aplikacja_dla_strzelcowTheme
import androidx.compose.material3.*
import java.text.SimpleDateFormat
import java.util.*


class SeriesDetailsActivity : ComponentActivity() {

    private val repository = FirestoreRepository()
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionId = intent.getStringExtra("sessionId") ?: return
        val seriesId = intent.getStringExtra("seriesId") ?: return

        setContent {
            var series by remember { mutableStateOf<Series?>(null) }
            var shots by remember { mutableStateOf<List<Shot>>(emptyList()) }

            LaunchedEffect(seriesId) {
                repository.getSingleSeriesWithShots(sessionId, seriesId) { s, sh ->
                    series = s
                    shots = sh
                }
            }

            // Obliczamy wynik
            val totalScore = shots.sumOf { it.value }
            val maxScore = if (shots.isNotEmpty()) shots.size * 10 else 0

            Scaffold(
                topBar = {
                    TopAppBar(title = { Text("Szczegóły Serii") })
                }
            ) { padding ->
                if (series == null) {
                    // Ekran ładowania
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(padding), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    // Główna zawartość
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // ZDJĘCIE TARCZY
                        if (!series!!.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = series!!.imageUrl,
                                contentDescription = "Zdjęcie tarczy",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .padding(vertical = 8.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        // WYNIK
                        Text("Wynik: $totalScore / $maxScore", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(16.dp))

                        // LISTA STRZAŁÓW
                        Text("Strzały:", style = MaterialTheme.typography.titleMedium)
                        LazyRow(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(shots) { index, shot ->
                                Chip(label = "${index + 1}. ${shot.value}")
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))


                        // PARAMETRY SERII
                        Text("Parametry:", style = MaterialTheme.typography.titleMedium)
                        InfoRow(label = "Broń:", value = series!!.weapon)
                        InfoRow(label = "Amunicja:", value = series!!.ammo)
                        InfoRow(label = "Dystans:", value = "${series!!.distance} m")
                        if (series!!.notes.isNotBlank()) {
                            InfoRow(label = "Opis:", value = series!!.notes)
                        }
                        val formattedDate = series!!.createdAt?.toDate()
                            ?.let { SimpleDateFormat("dd.MM.yyyy HH:mm",
                                Locale.getDefault()).format(it) } ?: "Brak daty"
                        InfoRow(label = "Data:", value = formattedDate)

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// Pomocniczy Composable do wyświetlania wierszy z informacjami
@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp))
        Text(value)
    }
}

// Pomocniczy Composable dla Chipa ze strzałem
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Chip(label: String) {
    SuggestionChip(onClick = { /* Można tu dodać np. podświetlenie na obrazku */ }, label = { Text(label) })
}
//    private lateinit var sessionId: String
//    private lateinit var seriesId: String
//
//
//    private var seriesState by mutableStateOf<Series?>(null)
//    private var shotsState by mutableStateOf<List<Shot>>(emptyList())
//
//    private fun loadData() {
//        repository.getSeries(sessionId) { list ->
//            seriesState = list.find { it.id == seriesId }
//        }
//
//        repository.getShots(sessionId, seriesId) {
//            shotsState = it
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//
//        sessionId = intent.getStringExtra("sessionId") ?: return
//        seriesId = intent.getStringExtra("seriesId") ?: return
//
//        setContent {
//            val context = LocalContext.current
//            val series = seriesState
//            val shots = shotsState
//
//            Aplikacja_dla_strzelcowTheme {
//                Column(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .padding(16.dp)
//                ) {
//
//                    Text("Szczegóły serii")
//
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    // 📸 ZDJĘCIE TARCZY
//                    if (series?.imageUrl != null) {
//                        AsyncImage(
//                            model = series.imageUrl,
//                            contentDescription = "Tarcza",
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .height(300.dp)
//                        )
//
//                        Spacer(modifier = Modifier.height(16.dp))
//                    }
//
//                    Button(
//                        onClick = {
//                            context.startActivity(
//                                Intent(context, AddShotActivity::class.java).apply {
//                                    putExtra("sessionId", sessionId)
//                                    putExtra("seriesId", seriesId)
//                                }
//                            )
//                        }
//                    ) {
//                        Text("Dodaj strzał")
//                    }
//
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    LazyColumn {
//                        items(shots) { shot ->
//                            ShotItem(shot)
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        loadData()
//    }
//}
//
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
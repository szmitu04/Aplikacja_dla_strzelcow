package com.example.aplikacja_dla_strzelcow


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
import androidx.compose.ui.unit.dp
import com.example.aplikacja_dla_strzelcow.data.FirestoreRepository
import com.example.aplikacja_dla_strzelcow.data.Series
import com.example.aplikacja_dla_strzelcow.ui.theme.Aplikacja_dla_strzelcowTheme
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import java.util.Locale
//import androidx.wear.compose.foundation.size
import com.example.aplikacja_dla_strzelcow.data.Shot
import java.util.*
import androidx.compose.material3.*
import java.text.SimpleDateFormat
import java.util.*
class SessionDetailsActivity : ComponentActivity() {

    private val repository = FirestoreRepository()
    private lateinit var sessionId: String
    //private var seriesState by mutableStateOf<List<Series>>(emptyList())
    // Zmieniamy stan, aby przechowywał parę: Seria + Lista Strzałów
    private var seriesListState by mutableStateOf<List<Pair<Series, List<Shot>>>>(emptyList())

    private fun loadSeries() {
//        repository.getSeries(sessionId) {
//            seriesState = it
//        }
        // Używamy nowej funkcji, która pobiera wszystko za jednym razem
        repository.getSeriesWithShots(sessionId) {
            seriesListState = it
        }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sessionId = intent.getStringExtra("sessionId") ?: return

        setContent {
            val context = LocalContext.current
            val seriesWithShots = seriesListState
            //val series = seriesState
            //var series by remember { mutableStateOf<List<Series>>(emptyList()) }


            Scaffold(
                topBar = { TopAppBar(title = { Text("Serie w Treningu") }) },
                floatingActionButton = {
                    FloatingActionButton(onClick = {
                        context.startActivity(
                            Intent(context, AddSeriesActivity::class.java).apply {
                                putExtra("sessionId", sessionId)
                            }
                        )
                    }) {
                        Text("Dodaj serię") // Lepszy tekst dla FAB
                    }
                }
            ) { padding ->
                if (seriesWithShots.isEmpty()) {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(padding), contentAlignment = Alignment.Center) {
                        Text("Brak serii. Dodaj pierwszą!")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp)
                    ) {
                        items(seriesWithShots) { (series, shots) ->
                            SeriesItem(
                                series = series,
                                shots = shots,
                                onClick = {
                                    context.startActivity(
                                        Intent(context, SeriesDetailsActivity::class.java).apply {
                                            putExtra("sessionId", sessionId)
                                            putExtra("seriesId", series.id)
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }

//            Aplikacja_dla_strzelcowTheme {
//                Column(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .padding(16.dp)
//                ) {
//
//                    Text("Serie w sesji")
//
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    Button(
//                        onClick = {
//                            context.startActivity(
//                                Intent(context, AddSeriesActivity::class.java).apply {
//                                    putExtra("sessionId", sessionId)
//                                }
//                            )
//                        }
//                    ) {
//                        Text("Dodaj serię")
//                    }
//
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    LazyColumn {
//                        items(series) { s ->
//                            SeriesItem(s) {
//                                context.startActivity(
//                                    Intent(context, SeriesDetailsActivity::class.java).apply {
//                                        putExtra("sessionId", sessionId)
//                                        putExtra("seriesId", s.id)
//                                    }
//                                )
//                            }
//                        }
//                    }
//                }
//            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadSeries()
    }

}

@Composable
fun SeriesItem(series: Series, shots: List<Shot>, onClick: () -> Unit) {
    val totalScore = shots.sumOf { it.value }
    val maxScore = shots.size * 10
    val formattedDate = series.createdAt?.toDate()
        ?.let { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(it) } ?: "Brak daty"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(series.weapon.ifBlank { "Brak broni" }, style = MaterialTheme.typography.titleLarge)
            Text(formattedDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Wynik: $totalScore / $maxScore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(12.dp)
//            .clickable { onClick() }
//    ) {
//        Text("Broń: ${series.weapon}")
//        Text("Amunicja: ${series.ammo}")
//        Text("Dystans: ${series.distance} m")
//    }
}
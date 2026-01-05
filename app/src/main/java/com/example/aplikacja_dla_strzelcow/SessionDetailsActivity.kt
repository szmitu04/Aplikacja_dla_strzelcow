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
class SessionDetailsActivity : ComponentActivity() {

    private val repository = FirestoreRepository()
    private lateinit var sessionId: String
    private var seriesState by mutableStateOf<List<Series>>(emptyList())
    private fun loadSeries() {
        repository.getSeries(sessionId) {
            seriesState = it
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sessionId = intent.getStringExtra("sessionId") ?: return

        setContent {
            val context = LocalContext.current
            val series = seriesState
            //var series by remember { mutableStateOf<List<Series>>(emptyList()) }



            Aplikacja_dla_strzelcowTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {

                    Text("Serie w sesji")

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(context, AddSeriesActivity::class.java).apply {
                                    putExtra("sessionId", sessionId)
                                }
                            )
                        }
                    ) {
                        Text("Dodaj serię")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn {
                        items(series) { s ->
                            SeriesItem(s) {
                                context.startActivity(
                                    Intent(context, SeriesDetailsActivity::class.java).apply {
                                        putExtra("sessionId", sessionId)
                                        putExtra("seriesId", s.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadSeries()
    }

}

@Composable
fun SeriesItem(series: Series, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clickable { onClick() }
    ) {
        Text("Broń: ${series.weapon}")
        Text("Amunicja: ${series.ammo}")
        Text("Dystans: ${series.distance} m")
    }
}
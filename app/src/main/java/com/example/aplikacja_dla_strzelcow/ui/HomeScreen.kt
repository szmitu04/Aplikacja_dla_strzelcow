package com.example.aplikacja_dla_strzelcow.ui



import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aplikacja_dla_strzelcow.data.Session
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.example.aplikacja_dla_strzelcow.AddSessionActivity
import com.example.aplikacja_dla_strzelcow.ui.statistics.StatisticsActivity
import com.example.aplikacja_dla_strzelcow.data.FirestoreRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.content.Intent
@Composable
fun HomeScreen(
    sessions: List<Session>,
    onSessionClick: (Session) -> Unit,
    onNewTraining: () -> Unit,
    repository: FirestoreRepository
) {
    val context = LocalContext.current

    //  NOWY STAN DLA ŚREDNIEGO WYNIKU
    var averageScore by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(Unit) {
        // Obliczamy datę sprzed 30 dni
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -30)
        val startDate = calendar.time
        repository.getShotsSince(startDate) { shots ->
            if (shots.isNotEmpty()) {
                averageScore = shots.map { it.value }.average()
            } else {
                averageScore = 0.0 // Ustaw 0.0, jeśli brak strzałów
            }
        }
    }



    LazyColumn (
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            StatisticsCard(averageScore = averageScore) {
                // Nawigacja donowego ekranu statystyk
                               context.startActivity(Intent(context, StatisticsActivity::class.java))
                          }
                       }

        // KAFEL STATYSTYKI – placeholder
//        Card(
//            modifier = Modifier.fillMaxWidth(),
//            colors = CardDefaults.cardColors(
//                containerColor = MaterialTheme.colorScheme.surfaceVariant
//            )
//        ) {
//            Column(
//                modifier = Modifier.padding(24.dp)
//            ) {
//                Text(
//                    text = "Statystyki (placeholder)",
//                    style = MaterialTheme.typography.titleMedium
//                )
//            }
//        }

        //Spacer(modifier = Modifier.height(16.dp))
item{
        Button(
            onClick = onNewTraining,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Nowy trening")
        }
}
        //Spacer(modifier = Modifier.height(24.dp))
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Poprzednie treningi",
                style = MaterialTheme.typography.titleMedium
            )
        }
        //Spacer(modifier = Modifier.height(8.dp))

        // --- LISTA TRENINGÓW ---
        items(sessions) { session ->
            SessionItem(
                session = session,
                onClick = { onSessionClick(session) }
            )
        }
    }
}
@Composable
private fun StatisticsCard(averageScore: Double?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ){
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Statystyki", style = MaterialTheme.typography.titleMedium)
                Text("Średni wynik z 30 dni:", style = MaterialTheme.typography.bodySmall)
                if (averageScore == null) {
                    // Placeholder ładowania
                    Text("...", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                } else {
                    // Wyświetlamy do jednego miejsca po przecinku
                    Text(
                        text = "%.1f / 10".format(averageScore),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Przejdź do statystyk",
                modifier = Modifier.size(32.dp)
            )
        }
    }}




@Composable
private fun SessionItem(
    session: Session,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = session.location,
                fontWeight = FontWeight.Bold
            )

            session.createdAt?.toDate()?.let {
                Text("Data: $it")
            }
        }
    }
}
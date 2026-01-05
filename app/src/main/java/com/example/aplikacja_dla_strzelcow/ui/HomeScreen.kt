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

@Composable
fun HomeScreen(
    sessions: List<Session>,
    onSessionClick: (Session) -> Unit,
    onNewTraining: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // KAFEL STATYSTYKI – placeholder
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Statystyki (placeholder)",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNewTraining,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Nowy trening")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Poprzednie treningi",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(sessions) { session ->
                SessionItem(
                    session = session,
                    onClick = { onSessionClick(session) }
                )
            }
        }
    }
}

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
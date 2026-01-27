package com.example.aplikacja_dla_strzelcow.ui


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import com.example.aplikacja_dla_strzelcow.data.FirestoreRepository
import com.example.aplikacja_dla_strzelcow.data.SettingsManager
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit
) {
    // Pobieramy context, aby uzyskać dostęp do SettingsManager
    val context = LocalContext.current
    val repository = remember { FirestoreRepository() }
    val settingsManager = remember { SettingsManager(context) }

    // Stany dla list
    var weaponsList by remember { mutableStateOf<List<String>>(emptyList()) }
    var ammoList by remember { mutableStateOf<List<String>>(emptyList()) }

    // Stany do zarządzania dialogami
    var showAddDialog by remember { mutableStateOf<String?>(null) } // "weapon" lub "ammo"
    var showDeleteDialog by remember { mutableStateOf<Pair<String, String>?>(null) } // Typ i nazwa
    var showThresholdDialog by remember { mutableStateOf(false) }

    // Stany do zarządzania dialogiem i wartością progu
    var showDialog by remember { mutableStateOf(false) }
    var currentThreshold by remember { mutableStateOf(settingsManager.getThreshold().toString()) }

    // Ładujemy dane przy pierwszym uruchomieniu
    LaunchedEffect(Unit) {
        repository.getEquipmentLists { weapons, ammo ->
            weaponsList = weapons
            ammoList = ammo
        }
    }

//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        verticalArrangement = Arrangement.Center
//    ) {
//
//        Text("Profil użytkownika")
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // Przycisk do zmiany progu
//        Button(onClick = { showDialog = true }) {
//            Text("Zmień próg detekcji (obecnie: ${settingsManager.getThreshold()})")
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        Button(onClick = onLogout) {
//            Text("Wyloguj się")
//        }
//    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- SEKCJA BRONI ---
        item {
            Text("Twoje Bronie", style = MaterialTheme.typography.titleLarge)
            Button(onClick = { showAddDialog = "weapon" }) { Text("Dodaj broń") }
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(weaponsList) { weapon ->
            ListItem(

                headlineContent = { Text(weapon) }, // Zamiast headlineText
                modifier = Modifier.clickable { showDeleteDialog =
                    Pair("weapon", weapon) }
            )
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }

        // --- SEKCJA AMUNICJI ---
        item {
            Text("Twoja Amunicja", style = MaterialTheme.typography.titleLarge)
            Button(onClick = { showAddDialog = "ammo" }) { Text("Dodaj amunicję") }
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(ammoList) { ammo ->
            ListItem(
                headlineContent = { Text(ammo) },
                modifier = Modifier.clickable { showDeleteDialog = Pair("ammo", ammo) }
            )
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }

        // --- USTAWIENIA I WYLOGOWANIE ---
        item {
            Button(onClick = { showThresholdDialog = true }) {
                Text("Zmień próg detekcji (obecnie: ${settingsManager.getThreshold()})")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onLogout) { Text("Wyloguj się") }
        }
    }

    // --- DIALOGI ---

    // Dialog do DODAWANIA
    if (showAddDialog != null) {
        var newItem by remember { mutableStateOf("") }
        val type = showAddDialog
        AlertDialog(
            onDismissRequest = { showAddDialog = null },
            title = { Text("Dodaj ${if (type == "weapon") "broń" else "amunicję"}") },
            text = {
                OutlinedTextField(
                    value = newItem,
                    onValueChange = { newItem = it },
                    label = { Text("Nazwa") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newItem.isNotBlank()) {
                            if (type == "weapon") {
                                val updatedList = weaponsList + newItem
                                repository.saveWeaponsList(updatedList)
                                weaponsList = updatedList
                            } else {
                                val updatedList = ammoList + newItem
                                repository.saveAmmoList(updatedList)
                                ammoList = updatedList
                            }
                        }
                        showAddDialog = null
                    }
                ) { Text("Zapisz") }
            }
        )
    }

    // Dialog do USUWANIA
    showDeleteDialog?.let { (type, item) ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Potwierdzenie") },
            text = { Text("Czy na pewno chcesz usunąć '$item'?") },
            confirmButton = {
                Button(
                    onClick = {
                        if (type == "weapon") {
                            val updatedList = weaponsList - item
                            repository.saveWeaponsList(updatedList)
                            weaponsList = updatedList
                        } else {
                            val updatedList = ammoList - item
                            repository.saveAmmoList(updatedList)
                            ammoList = updatedList
                        }
                        showDeleteDialog = null
                    }
                ) { Text("Tak, usuń") }
            },
            dismissButton = { Button(onClick = { showDeleteDialog = null }) { Text("Anuluj") } }
        )
    }

    // Dialog do progu detekcji (przeniesiony z poprzednich instrukcji)
    if (showThresholdDialog) {
        // Używamy `currentThreshold` zdefiniowanego na górze composable,
        // aby stan był spójny i przetrwał rekompozycje.
        var thresholdValue by remember { mutableStateOf(settingsManager.getThreshold().toString()) }

        AlertDialog(
            onDismissRequest = { showThresholdDialog = false },
            title = { Text("Zmień próg detekcji") },
            // Uzupełniamy brakujący parametr 'text'
            text = {
                Column {
                    Text(
                        "Wyższa wartość = większa czułość (wykryje jaśniejsze punkty). Domyślnie: 20. Zalecane: 10-40.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = thresholdValue,
                        onValueChange = { thresholdValue = it },
                        label = { Text("Próg (0-255)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Zapisujemy nową wartość i zamykamy dialog
                        settingsManager.saveThreshold(
                            thresholdValue.toIntOrNull() ?: SettingsManager.DEFAULT_THRESHOLD
                        )
                        showThresholdDialog = false
                    }
                ) { Text("Zapisz") }
            },
            dismissButton = {
                // Dodajemy przycisk Anuluj dla lepszego UX
                Button(onClick = { showThresholdDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text("Profil użytkownika")

        Spacer(modifier = Modifier.height(16.dp))

        // Przycisk do zmiany progu
        Button(onClick = { showDialog = true }) {
            Text("Zmień próg detekcji (obecnie: ${settingsManager.getThreshold()})")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onLogout) {
            Text("Wyloguj się")
        }
    }
}
//    if (showDialog) {
//        AlertDialog(
//            onDismissRequest = { showDialog = false },
//            title = { Text("Zmień próg detekcji") },
//            text = {
//                Column {
//                    Text("Niższa wartość = większa czułość (wykryje jaśniejsze punkty). Domyślnie: 20.")
//                    OutlinedTextField(
//                        value = currentThreshold,
//                        onValueChange = { currentThreshold = it },
//                        label = { Text("Próg (0-255)") },
//                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
//                    )
//                }
//            },
//            confirmButton = {
//                Button(
//                    onClick = {
//                        val newThreshold = currentThreshold.toIntOrNull() ?: SettingsManager.DEFAULT_THRESHOLD
//                        settingsManager.saveThreshold(newThreshold)
//                        showDialog = false
//                    }
//                ) {
//                    Text("Zapisz")
//                }
//            },
//            dismissButton = {
//                Button(onClick = { showDialog = false }) {
//                    Text("Anuluj")
//                }
//            }
//        )
//    }
//}

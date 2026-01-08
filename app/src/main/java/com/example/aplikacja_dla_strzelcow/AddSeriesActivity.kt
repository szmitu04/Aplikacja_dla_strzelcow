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
import com.example.aplikacja_dla_strzelcow.data.TargetParams
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.aplikacja_dla_strzelcow.data.Series
import com.example.aplikacja_dla_strzelcow.data.Shot
import com.google.firebase.Timestamp
import kotlin.math.pow
import kotlin.math.sqrt
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.graphics.BitmapFactory
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.foundation.lazy.itemsIndexed // Upewnij się, że ten import jest

import androidx.compose.material3.ButtonDefaults // DODAJ
import androidx.compose.material3.ExperimentalMaterial3Api // DODAJ
import androidx.compose.material3.SuggestionChip // DODAJ
import androidx.compose.ui.graphics.asImageBitmap // DODAJ
import androidx.compose.ui.layout.ContentScale // DODAJ

import androidx.compose.ui.unit.toSize
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
                var overlayPhotoFile by remember { mutableStateOf<File?>(null) }
                var originalPhotoFile by remember { mutableStateOf<File?>(null) }
                // 🔴 DODANE: Zmienna stanu przechowująca wynik detekcji
                var detectionResult by remember { mutableStateOf<TargetParams?>(null) }

                var shotToEdit by remember { mutableStateOf<Shot?>(null) }
                var shots by remember { mutableStateOf<List<Shot>>(emptyList()) }
                var isAddingShotMode by remember { mutableStateOf(false) }
                var showAddShotDialog by remember { mutableStateOf<Offset?>(null) }
                val cameraLauncher =
                    rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == RESULT_OK) {
                            val overlayPath = result.data?.getStringExtra("overlayPhotoPath")
                            val data = result.data
                            if (overlayPath != null) {
                                overlayPhotoFile = File(overlayPath)
                            }
                            val originalPath = result.data?.getStringExtra("originalPhotoPath")
                            if (originalPath != null) {
                                originalPhotoFile = File(originalPath)
                            }
                            if (data != null) {
                                detectionResult = TargetParams(
                                    centerX = data.getFloatExtra("cx", 0f),
                                    centerY = data.getFloatExtra("cy", 0f),
                                    radius = data.getFloatExtra("radius", 0f)
                                )
                                //  Deserializujemy listę strzałów z JSONa
                                val shotsJson = data.getStringExtra("shotsJson")
                                if (shotsJson != null) {
                                    val gson = Gson()
                                    val listType = object : TypeToken<ArrayList<Shot>>() {}.type
                                    val detectedShots: List<Shot> = gson.fromJson(shotsJson, listType)

                                    // 2. Zamiast wywoływać Composable, po prostu zaktualizuj stan.
                                    // Interfejs użytkownika (ShotsList w głównym Column) sam się przerysuje.
                                    shots = detectedShots
                                }
                            }
                        }
                    }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
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
                        // Interaktywne zdjecie tarczy
                        TargetImageWithShots(
                            imageFile = overlayPhotoFile!!,
                            shots = shots,
                            targetParams = detectionResult,
                            isAddingShotMode = isAddingShotMode,
                            onAddShot = { tapOffset ->
                                showAddShotDialog = tapOffset
                                isAddingShotMode = false // Wyłącz tryb dodawania po kliknięciu
                            }
                        )



//                        AsyncImage(
//                            model = overlayPhotoFile, // Wyświetlamy zdjęcie z otoczką
//                            contentDescription = "Zdjęcie tarczy",
//                            modifier = Modifier.size(200.dp)
//                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
// --- SEKCJA PUNKTÓW/STRZAŁÓW ---
                    if (detectionResult != null) {
                        ShotsList(
                            shots = shots,
                            onAddClick = { isAddingShotMode = true },
                            onShotClick = { /* TODO: Edycja punktów */ }
                        )
                    }

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
                    // --- PODSUMOWANIE WYNIKU ---
                    if (shots.isNotEmpty()) {
                        val totalScore = shots.sumOf { it.value }
                        val maxScore = shots.size * 10
                        Text("Wynik: $totalScore / $maxScore")
                    }
//                    Button(
//                        onClick = {
//                            repository.createSeries(
//                                sessionId = sessionId,
//                                weapon = weapon,
//                                ammo = ammo,
//                                distance = distance.toIntOrNull() ?: 0,
//                                notes = notes, // Przekazujemy opis
//                                targetParams = detectionResult, // Przekazujemy dane z OpenCV
//                                onCreated = { seriesId ->
//
//                                // Używamy oryginalnego pliku do wysłania
//                                originalPhotoFile?.let { file ->
//                                    repository.uploadSeriesImage(
//                                        sessionId,
//                                        seriesId,
//                                        file
//                                    ) { url ->
//                                        repository.updateSeriesImage(
//                                            sessionId,
//                                            seriesId,
//                                            url
//                                        )
//                                    }
//                                }
//
//                                finish()
//                            }
//                            )
//                        }
//                    )

                    Button(onClick = {
                        val newSeries = Series(
                            weapon = weapon,
                            ammo = ammo,
                            distance = distance.toIntOrNull() ?: 0,
                            notes = notes,
                            createdAt = Timestamp.now(),
                            targetParams = detectionResult
                        )
                        repository.createSeriesWithShots(sessionId, newSeries, shots) { seriesId ->
                            originalPhotoFile?.let { file ->
                                repository.uploadSeriesImage(sessionId, seriesId, file) { url ->
                                    repository.updateSeriesImage(sessionId, seriesId, url)
                                }
                            }
                            finish()
                        }
                    })
                    {
                        Text("Zapisz serię")
                    }
                }
                // --- DIALOG DODAWANIA WARTOŚCI STRZAŁU ---
                if (showAddShotDialog != null && detectionResult != null) {
                    AddShotValueDialog(
                        onDismiss = { showAddShotDialog = null },
                        onSave = { value ->
                            val (x, y) = convertPxToRelative(
                                tapOffset = showAddShotDialog!!,
                                imageSize = Size(
                                    overlayPhotoFile!!.inputStream().use { BitmapFactory.decodeStream(it) }.width.toFloat(),
                                    overlayPhotoFile!!.inputStream().use { BitmapFactory.decodeStream(it) }.height.toFloat()
                                ),
                                targetParams = detectionResult!!
                            )

                            shots = shots + Shot(x = x, y = y, value = value, timestamp = Timestamp.now())
                            showAddShotDialog = null
                        }
                    )
                }

                shotToEdit?.let { currentShot ->
                    EditShotDialog(
                        shotToEdit = currentShot,
                        onDismiss = { shotToEdit = null }, // Zamyka dialog
                        onSave = { newValue ->
                            // Znajdujemy strzał na liście i aktualizujemy jego wartość
                            val updatedShots = shots.map {
                                if (it.id == currentShot.id) {
                                    it.copy(value = newValue)
                                } else {
                                    it
                                }
                            }
                            shots = updatedShots
                            shotToEdit = null // Zamknij dialog po zapisie
                        },
                        onDelete = {
                            // Filtrujemy listę, aby usunąć ten strzał
                            shots = shots.filter { it.id != currentShot.id }
                            shotToEdit = null // Zamknij dialog po usunięciu
                        }
                    )
                }

            }
        }
    }
}

// 🔴 NOWY COMPOSABLE: Interaktywny obraz tarczy
@Composable
fun TargetImageWithShots(
    imageFile: File,
    shots: List<Shot>,
    targetParams: TargetParams?,
    isAddingShotMode: Boolean,
    onAddShot: (Offset) -> Unit
) {
    val bitmap = remember(imageFile) { loadBitmapWithRotation(imageFile) }
    var imageSize by remember { mutableStateOf(Size.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
            .pointerInput(isAddingShotMode, targetParams) {
                if (isAddingShotMode && targetParams != null) {
                    detectTapGestures { offset ->
                        onAddShot(offset)
                    }
                }
            }
    ) {
        AsyncImage(
            model = imageFile,
            contentDescription = "Zdjęcie tarczy",
            modifier = Modifier.fillMaxSize(),
            onSuccess = { state ->
                imageSize = Size(
                    state.painter.intrinsicSize.width,
                    state.painter.intrinsicSize.height
                )
            }
        )
        if (targetParams != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                shots.forEach { shot ->
                    val (px, py) = convertRelativeToPx(shot, imageSize, targetParams)
                    drawRect(
                        color = Color.Magenta,
                        topLeft = Offset(px - 15f, py - 15f),
                        size = Size(30f, 30f),
                        style = Stroke(width = 4f)
                    )
                }
            }
        }
        if (isAddingShotMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text("Dotknij miejsce przestrzeliny", color = Color.White)
            }
        }
    }
}

// 🔴 NOWY COMPOSABLE: Lista strzałów i przycisk "+"
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShotsList(
    shots: List<Shot>,
    onAddClick: () -> Unit,
    onShotClick: (Shot) -> Unit // Zmieniamy typ tego parametru
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(shots) { index, shot ->
            SuggestionChip(
                onClick = { onShotClick(shot) }, // Po kliknięciu wywołaj akcję
                label = { Text("${index + 1}. ${shot.value}") }
            )
        }
        item {
            Button(onClick = onAddClick) { // Przycisk "+" zostaje!
                Text("+")
            }
        }
    }
}

// 🔴 NOWY COMPOSABLE: Dialog do wpisywania punktów
@Composable
fun AddShotValueDialog(onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wprowadź wartość punktową") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Punkty") }
            )
        },
        confirmButton = {
            Button(onClick = { onSave(value.toIntOrNull() ?: 0) }) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

// --- FUNKCJE POMOCNICZE DO KONWERSJI WSPÓŁRZĘDNYCH ---

// Przelicza współrzędne względne strzału (-1..1) na piksele na obrazie
fun convertRelativeToPx(shot: Shot, imageSize: Size, targetParams: TargetParams): Offset {
    val norm = kotlin.math.min(imageSize.width, imageSize.height)
    val centerX_px = targetParams.centerX * imageSize.width
    val centerY_px = targetParams.centerY * imageSize.height
    val radius_px = targetParams.radius * norm

    val shotX_px = centerX_px + shot.x * radius_px
    val shotY_px = centerY_px + shot.y * radius_px
    return Offset(shotX_px, shotY_px)
}

// Przelicza piksele z kliknięcia na współrzędne względne tarczy
fun convertPxToRelative(tapOffset: Offset, imageSize: Size, targetParams: TargetParams): Pair<Float, Float> {
    val norm = kotlin.math.min(imageSize.width, imageSize.height)
    val centerX_px = targetParams.centerX * imageSize.width
    val centerY_px = targetParams.centerY * imageSize.height
    val radius_px = targetParams.radius * norm

    val relativeX = (tapOffset.x - centerX_px) / radius_px
    val relativeY = (tapOffset.y - centerY_px) / radius_px
    return Pair(relativeX, relativeY)
}
@Composable
fun EditShotDialog(
    shotToEdit: Shot,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
    onDelete: () -> Unit
) {
    var updatedValue by remember { mutableStateOf(shotToEdit.value.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edytuj strzał") },
        text = {
            OutlinedTextField(
                value = updatedValue,
                onValueChange = { updatedValue = it },
                label = { Text("Wartość punktowa") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(updatedValue.toIntOrNull() ?: shotToEdit.value)
                }
            ) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Usuń")
            }
        }
    )
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
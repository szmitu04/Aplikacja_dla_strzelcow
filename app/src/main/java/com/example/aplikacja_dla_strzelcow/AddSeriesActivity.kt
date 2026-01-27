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
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults

import androidx.compose.material3.ButtonDefaults // DODAJ
import androidx.compose.material3.ExperimentalMaterial3Api // DODAJ
import androidx.compose.material3.SuggestionChip // DODAJ
import androidx.compose.ui.graphics.asImageBitmap // DODAJ
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale // DODAJ
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.min

import androidx.compose.ui.unit.toSize
//import androidx.preference.forEach
import kotlin.math.min
@OptIn(ExperimentalMaterial3Api::class)
class AddSeriesActivity : ComponentActivity() {

    private val repository = FirestoreRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sessionId = intent.getStringExtra("sessionId") ?: return

        setContent {
            Aplikacja_dla_strzelcowTheme {
                val context = LocalContext.current
                var weapon by remember { mutableStateOf("Inna") }
                var ammo by remember { mutableStateOf("Inna") }
                var distance by remember { mutableStateOf("") }
                var notes by remember { mutableStateOf("") }
                var overlayPhotoFile by remember { mutableStateOf<File?>(null) }
                var originalPhotoFile by remember { mutableStateOf<File?>(null) }
                // 🔴 DODANE: Zmienna stanu przechowująca wynik detekcji
                var detectionResult by remember { mutableStateOf<TargetParams?>(null) }

                var originalBitmapSize by remember { mutableStateOf(Size.Zero) }
                var shotToEdit by remember { mutableStateOf<Shot?>(null) }
                var shots by remember { mutableStateOf<List<Shot>>(emptyList()) }
                var isAddingShotMode by remember { mutableStateOf(false) }
                var showAddShotDialog by remember { mutableStateOf<Offset?>(null) }
                var imageSizeOnScreen by remember { mutableStateOf(Size.Zero) }

                // Stany dla rozwijanych list
                var weaponsList by remember { mutableStateOf<List<String>>(emptyList()) }
                var ammoList by remember { mutableStateOf<List<String>>(emptyList()) }

                var weaponDropdownExpanded by remember { mutableStateOf(false) }
                var ammoDropdownExpanded by remember { mutableStateOf(false) }

// Ładujemy listy przy starcie
                LaunchedEffect(Unit) {
                    repository.getEquipmentLists { weapons, ammo ->
                        weaponsList = weapons + "Inna" // Dodajemy opcję "Inna"
                        ammoList = ammo + "Inna"
                    }
                }

                val cameraLauncher =
                    rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == RESULT_OK) {
                            val overlayPath = result.data?.getStringExtra("overlayPhotoPath")
                            val data = result.data
                            if (overlayPath != null) {

                                // 🔴 ZAPISUJEMY ROZMIAR BITMAPY PO JEJ OTRZYMANIU

                                overlayPhotoFile = File(overlayPath)
                            }


                            val originalPath = result.data?.getStringExtra("originalPhotoPath")
                            if (originalPath != null) {
                                originalPhotoFile = File(originalPath)
                                // 🔴 ZAPISUJEMY ROZMIAR BITMAPY PO JEJ OTRZYMANIU
                                val options = BitmapFactory.Options()
                                options.inJustDecodeBounds = true // Nie ładuj całej bitmapy, tylko jej wymiary
                                BitmapFactory.decodeFile(originalPath, options)
                                originalBitmapSize = Size(options.outWidth.toFloat(), options.outHeight.toFloat())
//
                                //originalPhotoFile = File(originalPath)
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
                            // 🔴 ZMIANA 3: Przekazujemy nowe parametry
                            onAddShot = { tapOffset, realSize ->
                                showAddShotDialog = tapOffset
                                imageSizeOnScreen = realSize // Zapisujemy rozmiar do stanu
                                isAddingShotMode = false
                            },
                            onSizeChanged = { realSize ->
//                                imageSizeOnScreen = realSize // Zapisujemy rozmiar do stanu
                            }
                        )
//                        TargetImageWithShots(
//                            imageFile = overlayPhotoFile!!,
//                            shots = shots,
//                            targetParams = detectionResult,
//                            isAddingShotMode = isAddingShotMode,
//                            onAddShot = { tapOffset ->
//                                showAddShotDialog = tapOffset
//                                isAddingShotMode = false // Wyłącz tryb dodawania po kliknięciu
//                            }
//                        )



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
                            onShotClick = { clickedShot ->
                                shotToEdit = clickedShot
                            }
                        )
                    }

                    Box {
                        // Używamy `ExposedDropdownMenuBox` - to jest zalecany sposób tworzenia takich pól
                        ExposedDropdownMenuBox(
                            expanded = weaponDropdownExpanded,
                            onExpandedChange = { weaponDropdownExpanded = !weaponDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = weapon,
                                onValueChange = {}, // Pusta lambda, bo pole jest tylko do odczytu
                                label = { Text("Broń") },
                                readOnly = true, // `readOnly = true` jest wymagane przez ExposedDropdownMenuBox
                                trailingIcon = {
                                    // Dodajemy ikonę strzałki dla lepszego UX
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = weaponDropdownExpanded)
                                },
                                modifier = Modifier
                                    .menuAnchor() // Łączy pole tekstowe z menu
                                    .fillMaxWidth()
                            )
                            // Menu, które się rozwija
                            ExposedDropdownMenu(
                                expanded = weaponDropdownExpanded,
                                onDismissRequest = { weaponDropdownExpanded = false }
                            ) {
                                weaponsList.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item) },
                                        onClick = {
                                            weapon = item
                                            weaponDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp)) // Dodajemy trochę przestrzeni

// --- ZASTĄP BLOK "AMUNICJA" PONIŻSZYM KODEM ---
                    Box {
                        ExposedDropdownMenuBox(
                            expanded = ammoDropdownExpanded,
                            onExpandedChange = { ammoDropdownExpanded = !ammoDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = ammo,
                                onValueChange = {},
                                label = { Text("Amunicja") },
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = ammoDropdownExpanded)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = ammoDropdownExpanded,
                                onDismissRequest = { ammoDropdownExpanded = false }
                            ) {
                                ammoList.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item) },
                                        onClick = {
                                            ammo = item
                                            ammoDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }



//                    OutlinedTextField(
//                        value = weapon,
//                        onValueChange = { weapon = it },
//                        label = { Text("Broń") },
//                        modifier = Modifier.fillMaxWidth()
//                    )
//
//                    OutlinedTextField(
//                        value = ammo,
//                        onValueChange = { ammo = it },
//                        label = { Text("Amunicja") },
//                        modifier = Modifier.fillMaxWidth()
//                    )

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
                            val (x, y) = convertTapToRelative(
                                tapOffset = showAddShotDialog!!,
                                // Używamy poprawnego rozmiaru zapisanego w stanie
                                composableSize = imageSizeOnScreen, // Rozmiar Composable, w którym kliknięto
                                originalBitmapSize = originalBitmapSize, // Rozmiar oryginalnego pliku zdjęcia
                                targetParams = detectionResult!!

                                //imageSizeOnScreen = imageSizeOnScreen,
                                //targetParams = detectionResult!!
                            )

                            shots = shots + Shot(x = x, y = y, value = value, timestamp = Timestamp.now(), isManual = true)
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
    onAddShot: (tapOffset: Offset, imageSize: Size) -> Unit,
    //onAddShot: (Offset) -> Unit
    onSizeChanged: (Size) -> Unit
) {
    val bitmap = remember(imageFile) { loadBitmapWithRotation(imageFile) }
    //var imageSize by remember { mutableStateOf(Size.Zero) }

    val bitmapOptions = remember(imageFile) {
        BitmapFactory.Options().apply { inJustDecodeBounds = true }
            .also { BitmapFactory.decodeFile(imageFile.path, it) }
    }
    val imageAspectRatio = if (bitmapOptions.outHeight > 0) {
        bitmapOptions.outWidth.toFloat() / bitmapOptions.outHeight.toFloat()
    } else {
        1f // Domyślnie kwadrat, jeśli nie uda się odczytać proporcji
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Utrzymujemy kwadratowe proporcje
            //.aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
            .pointerInput(isAddingShotMode, targetParams) {
                if (isAddingShotMode && targetParams != null) {
                    detectTapGestures { offset ->
                        onAddShot(offset, this.size.toSize())
                        // onAddShot(offset)
                    }
                }
            }
    ) {
        AsyncImage(
            model = imageFile,
            contentDescription = "Zdjęcie tarczy",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit, // Ważne, aby obraz był dopasowany
            onSuccess = { state ->
                onSizeChanged(state.painter.intrinsicSize)
//                imageSize = Size(
//                    state.painter.intrinsicSize.width,
//                    state.painter.intrinsicSize.height
//                )
            }
        )
        //rysowanie ramek, zielone dla automatycznych, rowe dla rozowe dla recznych
        if (targetParams != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Pobieramy rozmiar oryginalnej bitmapy (potrzebujemy go do obliczenia proporcji)
                val bitmapOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(imageFile.path, bitmapOptions)
                val bitmapWidth = bitmapOptions.outWidth.toFloat()
                val bitmapHeight = bitmapOptions.outHeight.toFloat()

                if (bitmapWidth == 0f || bitmapHeight == 0f) return@Canvas

                // 1. Obliczamy skalę i przesunięcie (offset), tak jak robi to `ContentScale.Fit`
                val scaleX = canvasWidth / bitmapWidth
                val scaleY = canvasHeight / bitmapHeight
                val scale = min(scaleX, scaleY)

                val scaledWidth = bitmapWidth * scale
                val scaledHeight = bitmapHeight * scale

                val offsetX = (canvasWidth - scaledWidth) / 2f
                val offsetY = (canvasHeight - scaledHeight) / 2f

                // Przygotowujemy Paint do rysowania tekstu
                val textPaint = android.graphics.Paint().apply {
                    textSize = 40f
                    style = android.graphics.Paint.Style.FILL
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    setShadowLayer(5f, 1f, 1f, android.graphics.Color.BLACK)
                }
                shots.forEachIndexed { index, shot ->
                    // Przeliczamy względne koordynaty strzału na piksele na *przeskalowanej bitmapie*
                    val shotXOnScaledBitmap = targetParams.centerX * scaledWidth + shot.x * (targetParams.radius * min(scaledWidth, scaledHeight))
                    val shotYOnScaledBitmap = targetParams.centerY * scaledHeight + shot.y * (targetParams.radius * min(scaledWidth, scaledHeight))

                    // Dodajemy offset, aby umieścić ramkę w poprawnym miejscu na canvasie
                    val finalX = shotXOnScaledBitmap + offsetX
                    val finalY = shotYOnScaledBitmap + offsetY

                    val color = if (shot.isManual) Color.Magenta else Color.Green

                    drawRect(
                        color = color,
                        topLeft = Offset(finalX - 15f, finalY - 15f),
                        size = Size(30f, 30f),
                        style = Stroke(width = 4f)
                    )
                    // 🔴 NOWOŚĆ: Rysowanie numeru w odpowiednim kolorze 🔴
                    val shotNumber = (index + 1).toString()
                    textPaint.color = color.toArgb() // Ustawiamy kolor tekstu taki sam jak ramki

                    // Rysujemy numer poniżej ramki
                    drawContext.canvas.nativeCanvas.drawText(
                        shotNumber,
                        finalX,
                        finalY + 45f, // Dostosuj pozycję Y, aby numer był pod ramką
                        textPaint
                    )
                }

//                shots.forEach { shot ->
//                    if (shot.isManual) {
//                        // Rysuj RÓŻOWE ramki dla strzałów ręcznych
//                        // Używamy `convertRelativeToPxSimple`, bo ręczne dodawanie już poprawnie przeliczyło koordynaty
//                        val (px, py) = convertRelativeToPxSimple(shot, canvasSize, targetParams)
//                        drawRect(
//                            color = Color.Magenta,
//                            topLeft = Offset(px - 15f, py - 15f),
//                            size = Size(30f, 30f),
//                            style = Stroke(width = 4f)
//                        )
//                    } else {
//                        // Rysuj ZIELONE ramki dla strzałów automatycznych
//                        // Używamy `convertRelativeToPxSimple`, która nie powoduje przesunięcia
//                        val (px, py) = convertRelativeToPxSimple(shot, canvasSize, targetParams)
//                        drawRect(
//                            color = Color.Green,
//                            topLeft = Offset(px - 15f, py - 15f),
//                            size = Size(30f, 30f),
//                            style = Stroke(width = 4f)
//                        )
//                    }
//                }
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
fun convertRelativeToPx(shot: Shot, imageSizeInPixels: Size, targetParams: TargetParams): Offset {
    val norm = min(imageSizeInPixels.width, imageSizeInPixels.height)
    val centerX_px = targetParams.centerX * imageSizeInPixels.width
    val centerY_px = targetParams.centerY * imageSizeInPixels.height
    val radius_px = targetParams.radius * norm

    val shotX_px = centerX_px + shot.x * radius_px
    val shotY_px = centerY_px + shot.y * radius_px
    return Offset(shotX_px, shotY_px)
}

// NOWA/STARA funkcja do rysowania ZIELONYCH ramek (automatycznych)
// Nie używa rozmiaru ekranu, bazuje na proporcjach.
fun convertRelativeToPxSimple(shot: Shot, composableSize: Size, targetParams: TargetParams): Offset {
    val norm = min(composableSize.width, composableSize.height)
    val centerX_px = targetParams.centerX * composableSize.width
    val centerY_px = targetParams.centerY * composableSize.height
    val radius_px = targetParams.radius * norm

    val shotX_px = centerX_px + shot.x * radius_px
    val shotY_px = centerY_px + shot.y * radius_px
    return Offset(shotX_px, shotY_px)
}


// Przelicza piksele z kliknięcia na współrzędne względne tarczy
fun convertPxToRelative(
    tapOffset: Offset,
    imageSizeOnScreen: Size, // Teraz to jest rozmiar Composable, a nie pliku
    targetParams: TargetParams
): Pair<Float, Float> {
    // Współrzędne kliknięcia są już w systemie koordynatów Composable
    val tapX = tapOffset.x
    val tapY = tapOffset.y

    // Przeliczamy znormalizowane parametry tarczy (0-1) na piksele
    // na podstawie realnego rozmiaru obrazu na ekranie
    val centerX_px = targetParams.centerX * imageSizeOnScreen.width
    val centerY_px = targetParams.centerY * imageSizeOnScreen.height
    val norm = min(imageSizeOnScreen.width, imageSizeOnScreen.height)
    val radius_px = targetParams.radius * norm

    // Obliczamy względne koordynaty strzału
    val relativeX = (tapX - centerX_px) / radius_px
    val relativeY = (tapY - centerY_px) / radius_px

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
// FUNKCJA TYLKO DO RĘCZNEGO DODAWANIA:
// Przelicza dotyk na ekranie na względne koordynaty, uwzględniając proporcje obrazu.
fun convertTapToRelative(
    tapOffset: Offset,
    composableSize: Size, // Rozmiar Composable (Box), na którym kliknięto
    originalBitmapSize: Size, // Rozmiar oryginalnego pliku zdjęcia
    targetParams: TargetParams
): Pair<Float, Float> {
    // 1. Sprawdzamy, czy mamy poprawne dane, aby uniknąć dzielenia przez zero.
    if (originalBitmapSize.width == 0f || originalBitmapSize.height == 0f) {
        return Pair(0f, 0f) // Zwróć bezpieczną wartość w razie błędu
    }
    if (targetParams.radius <= 0f) {
        return Pair(0f, 0f)
    }

    // 2. Obliczamy współczynniki skalowania i marginesy, aby odtworzyć `ContentScale.Fit`.
    val scaleX = composableSize.width / originalBitmapSize.width
    val scaleY = composableSize.height / originalBitmapSize.height
    val scale = min(scaleX, scaleY) // Skala dopasowania

    val scaledWidth = originalBitmapSize.width * scale
    val scaledHeight = originalBitmapSize.height * scale

    // Przesunięcie obrazu (marginesy), jeśli jest on wyśrodkowany w Composable.
    val offsetX = (composableSize.width - scaledWidth) / 2f
    val offsetY = (composableSize.height - scaledHeight) / 2f

    // 3. Przeliczamy współrzędne dotyku na Composable na odpowiadające im współrzędne na oryginalnej bitmapie.
    // To jest kluczowy krok: "usuwamy" marginesy i skalowanie z pozycji dotyku.
    val tapXOnBitmap = (tapOffset.x - offsetX) / scale
    val tapYOnBitmap = (tapOffset.y - offsetY) / scale

    // 4. Obliczamy współrzędne środka i promień tarczy w pikselach na oryginalnej bitmapie.
    val centerX_onBitmap = targetParams.centerX * originalBitmapSize.width
    val centerY_onBitmap = targetParams.centerY * originalBitmapSize.height
    val norm_onBitmap = min(originalBitmapSize.width, originalBitmapSize.height)
    val radius_onBitmap = targetParams.radius * norm_onBitmap

    // 5. Obliczamy finalne współrzędne względne, normalizując pozycję dotyku na bitmapie
    // względem środka i promienia tarczy na tej samej bitmapie.
    val relativeX = (tapXOnBitmap - centerX_onBitmap) / radius_onBitmap
    val relativeY = (tapYOnBitmap - centerY_onBitmap) / radius_onBitmap

    return Pair(relativeX, relativeY)
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
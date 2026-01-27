package com.example.aplikacja_dla_strzelcow.ui.statistics
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.aplikacja_dla_strzelcow.R
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign

import kotlin.math.min

class StatisticsActivity : ComponentActivity() {

    private val viewModel: StatisticsViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val uiState by viewModel.uiState.collectAsState()

            Scaffold(
                topBar = { TopAppBar(title = { Text("Statystyki i Analizy") }) }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // --- FILTRY ---
                    Text("Filtry", style = MaterialTheme.typography.titleLarge)
                    FilterSection(uiState, viewModel)

                    // --- TYP ANALIZY ---
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Rodzaj analizy", style = MaterialTheme.typography.titleMedium)
                    AnalysisTypeSelector(
                        selectedType = uiState.analysisType,
                        onTypeSelected = { viewModel.onAnalysisTypeChanged(it) }
                    )

                    // --- PRZYCISK GENEROWANIA ---
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.generateAnalysis() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("Generuj Analizę")
                        }
                    }

                    // --- OBSZAR WYNIKÓW ---
                    Spacer(modifier = Modifier.height(24.dp))
                    AnalysisResultView(uiState.analysisResult)
                }
            }
        }
    }
}


// --- KOMPONENTY UI DLA TEGO EKRANU ---

@Composable
fun FilterSection(uiState: StatisticsUiState, viewModel: StatisticsViewModel) {
    // TODO: Dodać przełącznik RadioButton/SegmentedButton dla Czas/Trening

    DropdownFilter(
        label = "Zakres czasu",
        options = TimeFilter.values().map { it.displayName },
        selectedOption = uiState.timeFilter.displayName,
        onOptionSelected = { selectedName ->
            TimeFilter.values().find { it.displayName == selectedName }?.let {
                viewModel.onTimeFilterChanged(it)
            }
        }
    )

    DropdownFilter(
        label = "Broń",
        options = uiState.availableWeapons,
        selectedOption = uiState.selectedWeapon,
        onOptionSelected = { viewModel.onWeaponChanged(it) }
    )

    DropdownFilter(
        label = "Amunicja",
        options = uiState.availableAmmo,
        selectedOption = uiState.selectedAmmo,
        onOptionSelected = { viewModel.onAmmoChanged(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownFilter(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AnalysisTypeSelector(selectedType: AnalysisType, onTypeSelected: (AnalysisType) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        AnalysisType.values().forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text(type.displayName) },
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun AnalysisResultView(result: AnalysisResult?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(top = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.target_background),
            contentDescription = "Tło tarczy",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        when (result) {
            is AnalysisResult.Heatmap -> {
                Image(
                    bitmap = result.bitmap.asImageBitmap(),
                    contentDescription = "Heatmapa strzałów",
                    modifier = Modifier.fillMaxSize()
                )
            }
            is AnalysisResult.Grouping -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasSizeInPx = min(size.width, size.height)
                    // Przelicznik ze współrzędnych względnych na piksele
                    val pixelsPerUnit = canvasSizeInPx / 2.0f / 3.0f

                    // Oblicz średni promień w pikselach
                    val radiusInPx = result.result.averageRadius * pixelsPerUnit

                    // Narysuj różowy okrąg skupienia
                    drawCircle(
                        color = Color.Magenta,
                        radius = radiusInPx,
                        center = center, // Środek Canvas
                        style = Stroke(width = 8f) // Gruba obręcz
                    )
                }
            }
            null -> {
                Text("Wybierz filtry i wygeneruj analizę", color = Color.White)
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    when (result) {
        is AnalysisResult.Grouping -> {
            Text(
                text = "Średnie skupienie: ${"%.2f".format(result.result.averageRadius)} pkt*",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "* Obliczono na podstawie ${result.result.seriesCount} serii.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        is AnalysisResult.Heatmap -> {
            Text(
                text = "Heatmapa pokazuje gęstość trafień.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        else -> {
            // Nie pokazuj nic, jeśli nie ma wyniku
        }
    }
}

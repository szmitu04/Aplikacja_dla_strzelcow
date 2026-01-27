package com.example.aplikacja_dla_strzelcow.ui.statistics


import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.example.aplikacja_dla_strzelcow.cv.HeatmapGenerator
import com.example.aplikacja_dla_strzelcow.data.FirestoreRepository
import com.example.aplikacja_dla_strzelcow.data.Series
import com.example.aplikacja_dla_strzelcow.data.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Calendar
import java.util.Date
import android.util.Log
import org.opencv.android.OpenCVLoader
import com.example.aplikacja_dla_strzelcow.cv.GroupingAnalysisResult
import com.example.aplikacja_dla_strzelcow.cv.GroupingAnalyzer
// --- KLASY STANU I ENUMY (ich miejsce jest tutaj) ---

data class StatisticsUiState(
    val timeFilter: TimeFilter = TimeFilter.LAST_30_DAYS,
    val trainingFilter: Session? = null,
    val selectedWeapon: String = "Wszystkie",
    val selectedAmmo: String = "Wszystkie",
    val analysisType: AnalysisType = AnalysisType.HEATMAP,
    val availableWeapons: List<String> = emptyList(),
    val availableAmmo: List<String> = emptyList(),
    val availableTrainings: List<Session> = emptyList(),
    val analysisResult: AnalysisResult? = null,
    val isLoading: Boolean = false
)

sealed class AnalysisResult {
    data class Heatmap(val bitmap: Bitmap) : AnalysisResult()
    data class Grouping(val result: GroupingAnalysisResult) : AnalysisResult()

}

enum class TimeFilter(val displayName: String) {
    LAST_24_HOURS("Ostatnie 24h"),
    LAST_7_DAYS("Ostatnie 7 dni"),
    LAST_30_DAYS("Ostatni miesiąc"),
    LAST_YEAR("Ostatni rok")
}

enum class AnalysisType(val displayName: String) {
    HEATMAP("Heatmapa"),
    GROUPING("Analiza skupienia")
}


// --- VIEWMODEL ---

class StatisticsViewModel : ViewModel() {
    private val repository = FirestoreRepository()

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        OpenCVLoader.initDebug()

        loadInitialData()
    }

    private fun loadInitialData() {
        repository.getEquipmentLists { weapons, ammo ->
            _uiState.update {
                it.copy(
                    availableWeapons = listOf("Wszystkie") + weapons,
                    availableAmmo = listOf("Wszystkie") + ammo
                )
            }
        }
        repository.getSessions { sessions ->
            _uiState.update { it.copy(availableTrainings = sessions) }
        }
    }

    fun onTimeFilterChanged(filter: TimeFilter) {
        _uiState.update { it.copy(timeFilter = filter, trainingFilter = null) }
    }

    fun onTrainingFilterChanged(session: Session) {
        _uiState.update { it.copy(trainingFilter = session) }
    }

    fun onWeaponChanged(weapon: String) {
        _uiState.update { it.copy(selectedWeapon = weapon) }
    }

    fun onAmmoChanged(ammo: String) {
        _uiState.update { it.copy(selectedAmmo = ammo) }
    }

    fun onAnalysisTypeChanged(type: AnalysisType) {
        _uiState.update { it.copy(analysisType = type) }
    }
/*
    fun generateAnalysis() {
        _uiState.update { it.copy(isLoading = true, analysisResult = null) }
        Log.d("ViewModelDebug", "Rozpoczęto generowanie analizy z filtrami: ${_uiState.value}")

        // 🔴 NOWA LOGIKA: Używamy prostej funkcji i filtrujemy w kodzie 🔴
        repository.getAllSeriesWithShots { allData ->
            Log.d("ViewModelDebug", "Otrzymano z Firestore: ${allData.size} serii.")

            val currentState = _uiState.value
            val startDate: Date? = if (currentState.trainingFilter == null) {
                val calendar = Calendar.getInstance()
                when (currentState.timeFilter) {
                    TimeFilter.LAST_24_HOURS -> calendar.apply { add(Calendar.HOUR, -24) }
                    TimeFilter.LAST_7_DAYS -> calendar.apply { add(Calendar.DAY_OF_YEAR, -7) }
                    TimeFilter.LAST_30_DAYS -> calendar.apply { add(Calendar.DAY_OF_YEAR, -30) }
                    TimeFilter.LAST_YEAR -> calendar.apply { add(Calendar.YEAR, -1) }
                }.time
            } else {
                null
            }

            // 1. Filtruj dane na podstawie wybranych opcji
            val filteredData = allData.filter { (series, shots) ->
                val dateMatch = startDate == null || (series.createdAt?.toDate()?.after(startDate) ?: false)
                val weaponMatch = currentState.selectedWeapon == "Wszystkie" || series.weapon == currentState.selectedWeapon
                val ammoMatch = currentState.selectedAmmo == "Wszystkie" || series.ammo == currentState.selectedAmmo
                // TODO: Dodaj filtr po treningu (sessionId)

                dateMatch && weaponMatch && ammoMatch
            }
            Log.d("ViewModelDebug", "Po przefiltrowaniu zostało: ${filteredData.size} serii.")

            // 2. Zbierz wszystkie strzały z przefiltrowanych serii
            val shotsToAnalyze = filteredData.flatMap { it.second }
            Log.d("ViewModelDebug", "Łączna liczba strzałów do analizy: ${shotsToAnalyze.size}.")

            // 3. Generuj analizę (ten kod pozostaje bez zmian)
            val analysisResult: AnalysisResult? = when (currentState.analysisType) {
                AnalysisType.HEATMAP -> {
                    if (shotsToAnalyze.isNotEmpty()) {
                        HeatmapGenerator.generate(shotsToAnalyze, width = 500, height = 500)?.let { bitmap ->
                            AnalysisResult.Heatmap(bitmap)
                        }
                    } else null
                }
                AnalysisType.GROUPING -> {
                    null
                }
            }
            Log.d("ViewModelDebug", "Wynik analizy: $analysisResult")

            // 4. Zaktualizuj UI
            _uiState.update { it.copy(isLoading = false, analysisResult = analysisResult) }
        }
    }
*/
fun generateAnalysis() {
    _uiState.update { it.copy(isLoading = true, analysisResult = null) }
    Log.d("ViewModelDebug", "Rozpoczęto generowanie analizy z filtrami: ${_uiState.value}")

    // 🔴 UŻYWAMY NOWEJ, NIEZAWODNEJ FUNKCJI 🔴
    repository.getAllTrainingsWithSeriesAndShots { allData ->
        Log.d("ViewModelDebug", "Otrzymano z Firestore: ${allData.size} serii (z wszystkich treningów).")

        val currentState = _uiState.value
        val startDate: Date? = if (currentState.trainingFilter == null) {
            val calendar = Calendar.getInstance()
            when (currentState.timeFilter) {
                TimeFilter.LAST_24_HOURS -> calendar.apply { add(Calendar.HOUR, -24) }
                TimeFilter.LAST_7_DAYS -> calendar.apply { add(Calendar.DAY_OF_YEAR, -7) }
                TimeFilter.LAST_30_DAYS -> calendar.apply { add(Calendar.DAY_OF_YEAR, -30) }
                TimeFilter.LAST_YEAR -> calendar.apply { add(Calendar.YEAR, -1) }
            }.time
        } else {
            null
        }

        // --- DALSZA CZĘŚĆ FUNKCJI POZOSTAJE BEZ ZMIAN ---

        // 1. Filtruj dane na podstawie wybranych opcji
        val filteredData = allData.filter { (series, _) ->
            val dateMatch = startDate == null || (series.createdAt?.toDate()?.after(startDate) ?: false)
            val weaponMatch = currentState.selectedWeapon == "Wszystkie" || series.weapon == currentState.selectedWeapon
            val ammoMatch = currentState.selectedAmmo == "Wszystkie" || series.ammo == currentState.selectedAmmo
            // TODO: Dodaj filtr po treningu (sessionId)

            dateMatch && weaponMatch && ammoMatch
        }
        Log.d("ViewModelDebug", "Po przefiltrowaniu zostało: ${filteredData.size} serii.")

        // 2. Zbierz wszystkie strzały z przefiltrowanych serii
        val shotsToAnalyze = filteredData.flatMap { it.second }
        Log.d("ViewModelDebug", "Łączna liczba strzałów do analizy: ${shotsToAnalyze.size}.")

        // 3. Generuj analizę
        val analysisResult: AnalysisResult? = when (currentState.analysisType) {
            AnalysisType.HEATMAP -> {
                if (shotsToAnalyze.isNotEmpty()) {
                    HeatmapGenerator.generate(shotsToAnalyze, width = 500, height = 500)?.let { bitmap ->
                        AnalysisResult.Heatmap(bitmap)
                    }
                } else null
            }
            AnalysisType.GROUPING -> {
                // Przekazujemy przefiltrowane serie (każda z listą swoich strzałów) do analizatora.
                GroupingAnalyzer.analyze(filteredData, requiredShotsInSeries = 5)?.let { result ->
                    AnalysisResult.Grouping(result)
                }
            }
        }
        Log.d("ViewModelDebug", "Wynik analizy: $analysisResult")

        // 4. Zaktualizuj UI
        _uiState.update { it.copy(isLoading = false, analysisResult = analysisResult) }
    }
}
}
package com.example.aplikacja_dla_strzelcow.ui.statistics

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.aplikacja_dla_strzelcow.cv.GroupingAnalyzer
import com.example.aplikacja_dla_strzelcow.cv.GroupingAnalysisResult
import com.example.aplikacja_dla_strzelcow.cv.HeatmapGenerator
import com.example.aplikacja_dla_strzelcow.data.FirestoreRepository
import com.example.aplikacja_dla_strzelcow.data.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.opencv.android.OpenCVLoader
import java.util.*

// --- KLASY STANU I ENUMY ---

enum class FilterMode(val displayName: String) {
    TIME("Zakres Czasu"),
    TRAINING("Trening")
}

data class StatisticsUiState(
    val filterMode: FilterMode = FilterMode.TIME,
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
) {
    val isTimeFilterActive: Boolean get() = filterMode == FilterMode.TIME
}

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
            _uiState.update {
                it.copy(
                    availableTrainings = sessions,
                    trainingFilter = sessions.firstOrNull()
                )
            }
        }
    }

    fun onFilterModeChanged(newMode: FilterMode) {
        _uiState.update { it.copy(filterMode = newMode) }
    }

    fun onTimeFilterChanged(filter: TimeFilter) {
        _uiState.update { it.copy(timeFilter = filter, filterMode = FilterMode.TIME) }
    }

    fun onTrainingFilterChanged(session: Session) {
        _uiState.update { it.copy(trainingFilter = session, filterMode = FilterMode.TRAINING) }
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

    // 🔴 POPRAWIONA I JEDYNA WERSJA FUNKCJI `generateAnalysis`
    fun generateAnalysis() {
        _uiState.update { it.copy(isLoading = true, analysisResult = null) }
        Log.d("ViewModelDebug", "Rozpoczęto generowanie analizy z filtrami: ${_uiState.value}")

        repository.getAllTrainingsWithSeriesAndShots { allData ->
            Log.d("ViewModelDebug", "Otrzymano z Firestore: ${allData.size} serii (z wszystkich treningów).")

            val currentState = _uiState.value

            val filteredData = allData.filter { (series, _) ->
                val primaryFilterMatch = if (currentState.isTimeFilterActive) {
                    val calendar = Calendar.getInstance()
                    val startDate = when (currentState.timeFilter) {
                        TimeFilter.LAST_24_HOURS -> calendar.apply { add(Calendar.HOUR, -24) }.time
                        TimeFilter.LAST_7_DAYS -> calendar.apply { add(Calendar.DAY_OF_YEAR, -7) }.time
                        TimeFilter.LAST_30_DAYS -> calendar.apply { add(Calendar.DAY_OF_YEAR, -30) }.time
                        TimeFilter.LAST_YEAR -> calendar.apply { add(Calendar.YEAR, -1) }.time
                    }
                    series.createdAt?.toDate()?.after(startDate) ?: false
                } else {
                    series.sessionId == currentState.trainingFilter?.id
                }

                val weaponMatch = currentState.selectedWeapon == "Wszystkie" || series.weapon == currentState.selectedWeapon
                val ammoMatch = currentState.selectedAmmo == "Wszystkie" || series.ammo == currentState.selectedAmmo

                primaryFilterMatch && weaponMatch && ammoMatch
            }
            Log.d("ViewModelDebug", "Po przefiltrowaniu zostało: ${filteredData.size} serii.")

            val analysisResult: AnalysisResult? = when (currentState.analysisType) {
                AnalysisType.HEATMAP -> {
                    // Użyj przefiltrowanych danych
                    val shotsToAnalyze = filteredData.flatMap { it.second }
                    Log.d("ViewModelDebug", "Łączna liczba strzałów do analizy HEATMAP: ${shotsToAnalyze.size}.")
                    if (shotsToAnalyze.isNotEmpty()) {
                        HeatmapGenerator.generate(shotsToAnalyze, width = 500, height = 500)?.let { bitmap ->
                            AnalysisResult.Heatmap(bitmap)
                        }
                    } else null
                }
                AnalysisType.GROUPING -> {
                    Log.d("ViewModelDebug", "Liczba serii do analizy GROUPING: ${filteredData.size}.")
                    // Użyj przefiltrowanych danych
                    GroupingAnalyzer.analyze(filteredData, requiredShotsInSeries = 5)?.let { result ->
                        AnalysisResult.Grouping(result)
                    }
                }
            }
            Log.d("ViewModelDebug", "Wynik analizy: $analysisResult")

            _uiState.update { it.copy(isLoading = false, analysisResult = analysisResult) }
        }
    }
}

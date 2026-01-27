package com.example.aplikacja_dla_strzelcow.cv


import com.example.aplikacja_dla_strzelcow.data.Series
import com.example.aplikacja_dla_strzelcow.data.Shot
import org.opencv.android.OpenCVLoader
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc

data class GroupingAnalysisResult(
    val averageRadius: Float, // Średni promień skupienia we współrzędnych względnych
    val seriesCount: Int      // Liczba serii wziętych pod uwagę
)

object GroupingAnalyzer {
    init {
        OpenCVLoader.initDebug()
    }

    /**
     * Analizuje skupienie dla podanych serii strzałów.
     * @param seriesWithShots Lista par (Seria, Strzały).
     * @param requiredShotsInSeries Wymagana liczba strzałów w serii, aby została wzięta pod uwagę.
     * @return Obiekt GroupingAnalysisResult lub null, jeśli żadna seria nie spełniła kryteriów.
     */
    fun analyze(
        seriesWithShots: List<Pair<Series, List<Shot>>>,
        requiredShotsInSeries: Int = 5 // Wartość domyślna, tak jak prosiłeś
    ): GroupingAnalysisResult? {
        // 1. Filtruj serie, aby zostawić tylko te z wymaganą liczbą strzałów.
        val validSeries = seriesWithShots.filter { it.second.size == requiredShotsInSeries }

        if (validSeries.isEmpty()) {
            return null // Jeśli żadna seria nie pasuje, nie ma co analizować.
        }

        // 2. Dla każdej ważnej serii, oblicz promień okręgu otaczającego.
        val radii = validSeries.map { (_, shots) ->
            // Konwertuj listę strzałów na listę punktów dla OpenCV
            val points = shots.map { Point(it.x.toDouble(), it.y.toDouble()) }
            val matOfPoints = MatOfPoint2f(*points.toTypedArray())

            // Oblicz minimalny okrąg otaczający
            val center = Point()
            val radiusArray = FloatArray(1)
            Imgproc.minEnclosingCircle(matOfPoints, center, radiusArray)

            // Zwróć promień
            radiusArray[0]
        }

        // 3. Oblicz średni promień ze wszystkich serii.
        val averageRadius = radii.average().toFloat()

        return GroupingAnalysisResult(
            averageRadius = averageRadius,
            seriesCount = validSeries.size
        )
    }
}
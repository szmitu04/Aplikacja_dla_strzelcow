package com.example.aplikacja_dla_strzelcow.cv

import android.graphics.Bitmap
import com.example.aplikacja_dla_strzelcow.data.Shot
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Scalar
import kotlin.math.min
object HeatmapGenerator {
    init {
        // Ta linia ładuje natywną bibliotekę OpenCV.
        // Musi zostać wykonana raz, zanim użyjemy jakiejkolwiek funkcji OpenCV.
        OpenCVLoader.initDebug()
    }
    /**
     * Generuje bitmapę z heatmapą na podstawie listy strzałów.
     * @param shots Lista strzałów, każdy ze współrzędnymi względnymi (-1..1).
     * @param width Szerokość docelowej bitmapy.
     * @param height Wysokość docelowej bitmapy.
     * @return Bitmapa z nałożoną heatmapą lub null, jeśli nie ma strzałów.
     */
    fun generate(shots: List<Shot>, width: Int, height: Int): Bitmap? {
        if (shots.isEmpty()) return null

        // 1. Stwórz czarną macierz (obraz) o podanych wymiarach.
        // Będzie to nasza "mapa gęstości".
        val densityMap = Mat.zeros(height, width, org.opencv.core.CvType.CV_32F)

        // 1. Zdefiniuj maksymalny względny promień, który chcemy zmapować na obraz.
        // Zgodnie z Twoim opisem, to jest 3.0.
        val MAX_RELATIVE_RADIUS = 3.0f

        // 2. Określ środek i promień naszej docelowej bitmapy w pikselach.
        val targetCenterX = width / 2.0
        val targetCenterY = height / 2.0
        // Promień w pikselach to połowa mniejszego wymiaru.
        val targetRadiusPx = min(width, height) / 2.0

        // 3. Oblicz, ile pikseli przypada na jedną jednostkę względną.
        // Dzielimy pełen promień w pikselach przez maksymalny promień względny.
        val pixelsPerUnit = targetRadiusPx / MAX_RELATIVE_RADIUS

        // 2. Dla każdego strzału, narysuj na mapie białą, rozmytą plamę (Gauss).
        // To symuluje "ciepło" wokół każdego trafienia.
        for (shot in shots) {
            // 4. Przelicz względne koordynaty strzału na absolutne piksele.
            // Mnożymy współrzędne strzału przez nasz nowy współczynnik skali.
            val x = targetCenterX + (shot.x * pixelsPerUnit)
            val y = targetCenterY + (shot.y * pixelsPerUnit)

            // Sprawdzenie, czy punkt mieści się w granicach, aby uniknąć crasha.
            if (x < 0 || x >= width || y < 0 || y >= height) {
                continue // Pomiń strzały, które są daleko poza tarczą.
            }

            val singleShotMap = Mat.zeros(height, width, CvType.CV_32F)
            Imgproc.circle(singleShotMap, Point(x, y), 1, Scalar(1.0), -1)

            // Dostosowanie rozmycia. Kernel (55, 55) i sigma (25) powinny
            // teraz dawać bardziej precyzyjne "plamy ciepła".
            Imgproc.GaussianBlur(singleShotMap, singleShotMap, Size(55.0, 55.0), 25.0)

            Core.add(densityMap, singleShotMap, densityMap)
        }

        // 3. Normalizuj mapę gęstości, aby wartości mieściły się w zakresie 0-255.
        // To jest potrzebne do pokolorowania.
        Core.normalize(densityMap, densityMap, 0.0, 255.0, Core.NORM_MINMAX)

        // 4. Przekonwertuj na 8-bitowy obraz i pokoloruj.
        val heatmap8bit = Mat()
        densityMap.convertTo(heatmap8bit, org.opencv.core.CvType.CV_8U)

        val colormap = Mat()
        // Używamy mapy kolorów 'JET' lub 'HOT', która daje przejście od niebieskiego/zielonego do żółtego/czerwonego.
        Imgproc.applyColorMap(heatmap8bit, colormap, Imgproc.COLORMAP_JET)

        // 5. Stwórz maskę, aby usunąć kolor tła (ciemnoniebieski).
        // Dzięki temu heatmapa będzie widoczna tylko tam, gdzie są strzały.
        val alphaChannel = Mat()
        densityMap.convertTo(alphaChannel, CvType.CV_8U) // Wartości 0-255

        // 2. Połącz kolorową mapę (RGB) z naszym kanałem Alfa (A) w jeden obraz RGBA.
        val finalHeatmap = Mat()
        val channels = listOf(colormap, alphaChannel)
        Core.merge(channels, finalHeatmap)

        // 3. Konwertuj finalną macierz OpenCV (która jest teraz w formacie BGRA)
        // na bitmapę Androida.
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // Użyj konwersji, która poprawnie interpretuje kanały (BGRA -> ARGB)
        Imgproc.cvtColor(finalHeatmap, finalHeatmap, Imgproc.COLOR_BGRA2RGBA)
        Utils.matToBitmap(finalHeatmap, bitmap)

        return bitmap
    }
}
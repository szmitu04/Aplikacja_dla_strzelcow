package com.example.aplikacja_dla_strzelcow.cv

import android.content.Context
import android.graphics.Bitmap
import com.example.aplikacja_dla_strzelcow.data.SettingsManager
import androidx.compose.ui.unit.min
import com.example.aplikacja_dla_strzelcow.data.Shot
import com.example.aplikacja_dla_strzelcow.data.TargetParams
import com.google.firebase.Timestamp
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.min
object ShotDetector {
    private const val MAX_RELATIVE_DISTANCE = 2.9f
    // Proporcja średnicy przestrzeliny do średnicy czarnego pola (0.1 = 1/10)
    private const val SHOT_DIAMETER_RATIO = 0.1f
    // Współczynniki tolerancji dla pola powierzchni
    private const val MIN_AREA_FACTOR = 0.3f
    private const val MAX_AREA_FACTOR = 2.0f

    fun detect(bitmap: Bitmap, targetParams: TargetParams, context: Context): List<Shot> {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        // 1. Konwersja do skali szarości (tak jak mówiłeś)
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)

        val settingsManager = SettingsManager(context)
        val thresholdValue = settingsManager.getThreshold().toDouble()

        // 2. Prognozowanie (Twój pomysł!)
        // Izolujemy tylko bardzo ciemne piksele (przestrzeliny). Wartość "10" można regulować.
        val threshold = Mat()
        Imgproc.threshold(gray, threshold, thresholdValue, 255.0, Imgproc.THRESH_BINARY_INV)

        // 3. Znajdowanie konturów (każda czarna plama to potencjalny strzał)
        val contours = ArrayList<MatOfPoint>()
        Imgproc.findContours(threshold, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)


// 1. Oblicz promień tarczy w pikselach
        val imageWidth = src.width().toFloat()
        val imageHeight = src.height().toFloat()
        val norm = min(imageWidth, imageHeight)

        // Promień czarnego pola w pikselach (to co wykrył TargetDetector)
        val blackFieldRadiusPx = targetParams.radius * norm
        //val targetRadiusPx = targetParams.radius * norm



        // Cała punktowana tarcza (pierścień "1") ma promień 65mm.
        // Czarne pole (pierścień "7") ma promień 2+7*4 = 30mm.
        // Proporcja: r_tarczy / r_czarnego_pola = 65 / 30 ≈ 2.167
        // Obliczamy promień całej tarczy w pikselach na podstawie tej proporcji.
        val wholeTargetRadiusPx = blackFieldRadiusPx * (65.0f / 23.0f)
        // Współrzędne środka tarczy w pikselach
        val targetCenterX_px = targetParams.centerX * imageWidth
        val targetCenterY_px = targetParams.centerY * imageHeight


        // Dynamiczne widełki dla pola przestrzeliny (logika bez zmian)
        val expectedShotRadiusPx = blackFieldRadiusPx * SHOT_DIAMETER_RATIO
        val expectedShotAreaPx = (PI * expectedShotRadiusPx.pow(2)).toFloat()
        val minShotArea = expectedShotAreaPx * MIN_AREA_FACTOR
        val maxShotArea = expectedShotAreaPx * MAX_AREA_FACTOR
//        // 2. Oblicz oczekiwany promień i pole przestrzeliny w pikselach
//        // Promień przestrzeliny = promień tarczy * proporcja średnic (co jest równe proporcji promieni)
//        val expectedShotRadiusPx = targetRadiusPx * SHOT_DIAMETER_RATIO
//        val expectedShotAreaPx = (PI * expectedShotRadiusPx.pow(2)).toFloat()
//
//        // 3. Oblicz dynamiczne widełki z uwzględnieniem tolerancji
//        val minShotArea = expectedShotAreaPx * MIN_AREA_FACTOR
//        val maxShotArea = expectedShotAreaPx * MAX_AREA_FACTOR

        val shots = mutableListOf<Shot>()
        val imageSize = Size(src.width().toDouble(), src.height().toDouble())

        contours.forEach { contour ->
            // Filtrujemy zbyt małe lub zbyt duże kontury, aby uniknąć szumu
            val area = Imgproc.contourArea(contour)
            if (area in minShotArea..maxShotArea) {
                val moments = Imgproc.moments(contour)
                if (moments.m00 > 0) { // Zabezpieczenie przed dzieleniem przez zero
                    // Środek masy konturu to pozycja strzału w pikselach
                    val centerX_px = (moments.m10 / moments.m00).toFloat()
                    val centerY_px = (moments.m01 / moments.m00).toFloat()
                    val score = ScoreCalculator.calculate(
                        shotX_px = centerX_px,
                        shotY_px = centerY_px,
                        targetCenterX_px = targetCenterX_px,
                        targetCenterY_px = targetCenterY_px,
                        target1RingRadiusPx = wholeTargetRadiusPx // Przekazujemy promień całej tarczy
                    )
                    // Konwertujemy współrzędne w pikselach na względne (znormalizowane)
                    val (relativeX, relativeY) = convertPxToRelative(
                        centerX_px, centerY_px, imageSize, targetParams
                    )
                    val distanceFromCenter = sqrt(relativeX.pow(2) + relativeY.pow(2))

                    // Obliczamy punkty (na razie będzie to 0)

                    if (distanceFromCenter < MAX_RELATIVE_DISTANCE) {
                        // Jeśli strzał jest wystarczająco blisko, dodajemy go
                        //val score = ScoreCalculator.calculate(relativeX, relativeY)
                        shots.add(
                            Shot(
                                x = relativeX,
                                y = relativeY,
                                value = score,
                                timestamp = Timestamp.now(),
                                isManual = false
                            )
                        )
                    }
                }
            }
        }
        return shots
    }

    // Funkcja pomocnicza do konwersji współrzędnych
    private fun convertPxToRelative(
        px: Float, py: Float, imageSize: Size, target: TargetParams
    ): Pair<Float, Float> {
        val norm = kotlin.math.min(imageSize.width, imageSize.height).toFloat()
        val centerX_px = target.centerX * imageSize.width.toFloat()
        val centerY_px = target.centerY * imageSize.height.toFloat()
        val radius_px = target.radius * norm

        val relativeX = (px - centerX_px) / radius_px
        val relativeY = (py - centerY_px) / radius_px
        return Pair(relativeX, relativeY)
    }
}
package com.example.aplikacja_dla_strzelcow.cv


import android.graphics.Bitmap
import com.example.aplikacja_dla_strzelcow.data.Shot
import com.example.aplikacja_dla_strzelcow.data.TargetParams
import com.google.firebase.Timestamp
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.pow
import kotlin.math.sqrt

object ShotDetector {
    private const val MAX_RELATIVE_DISTANCE = 2.9f
    fun detect(bitmap: Bitmap, targetParams: TargetParams): List<Shot> {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        // 1. Konwersja do skali szarości (tak jak mówiłeś)
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)

        // 2. Prognozowanie (Twój pomysł!)
        // Izolujemy tylko bardzo ciemne piksele (przestrzeliny). Wartość "10" można regulować.
        val threshold = Mat()
        Imgproc.threshold(gray, threshold, 11.0, 255.0, Imgproc.THRESH_BINARY_INV)

        // 3. Znajdowanie konturów (każda czarna plama to potencjalny strzał)
        val contours = ArrayList<MatOfPoint>()
        Imgproc.findContours(threshold, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        val shots = mutableListOf<Shot>()
        val imageSize = Size(src.width().toDouble(), src.height().toDouble())

        contours.forEach { contour ->
            // Filtrujemy zbyt małe lub zbyt duże kontury, aby uniknąć szumu
            val area = Imgproc.contourArea(contour)
            if (area in 200.0..3000.0) { // Te wartości trzeba będzie dostosować eksperymentalnie!
                val moments = Imgproc.moments(contour)
                // Środek masy konturu to pozycja strzału w pikselach
                val centerX_px = (moments.m10 / moments.m00).toFloat()
                val centerY_px = (moments.m01 / moments.m00).toFloat()

                // Konwertujemy współrzędne w pikselach na względne (znormalizowane)
                val (relativeX, relativeY) = convertPxToRelative(
                    centerX_px, centerY_px, imageSize, targetParams
                )
                val distanceFromCenter = sqrt(relativeX.pow(2) + relativeY.pow(2))

                // Obliczamy punkty (na razie będzie to 0)

                if (distanceFromCenter < MAX_RELATIVE_DISTANCE) {
                    // Jeśli strzał jest wystarczająco blisko, dodajemy go
                    val score = ScoreCalculator.calculate(relativeX, relativeY)
                    shots.add(
                        Shot(
                            x = relativeX,
                            y = relativeY,
                            value = score,
                            timestamp = Timestamp.now()
                        )
                    )
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
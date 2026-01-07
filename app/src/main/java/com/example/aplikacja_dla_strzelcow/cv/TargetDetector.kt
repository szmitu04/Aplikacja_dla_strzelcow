package com.example.aplikacja_dla_strzelcow.cv



import android.graphics.Bitmap
import org.opencv.android.OpenCVLoader // 👈 WAŻNE: Dodaj ten import
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.max


data class TargetDetectionResult(
    val centerX: Float,
    val centerY: Float,
    val radius: Float
)

object TargetDetector {
    init {
        // Ta linia ładuje natywną bibliotekę OpenCV.
        // Musi zostać wykonana raz, zanim użyjemy jakiejkolwiek funkcji OpenCV.
        OpenCVLoader.initDebug()
    }
    fun detect(bitmap: Bitmap): TargetDetectionResult? {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.GaussianBlur(src, src, Size(9.0, 9.0), 2.0)

        val thresh = Mat()
        Imgproc.threshold(
            src,
            thresh,
            0.0,
            255.0,
            Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU
        )

        val contours = ArrayList<MatOfPoint>()
        Imgproc.findContours(
            thresh,
            contours,
            Mat(),
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        if (contours.isEmpty()) return null

        val biggest = contours.maxByOrNull { Imgproc.contourArea(it) } ?: return null

        val points = MatOfPoint2f(*biggest.toArray())
        val center = Point()
        val radius = FloatArray(1)

        Imgproc.minEnclosingCircle(points, center, radius)

        val w = src.width().toFloat()
        val h = src.height().toFloat()
        val norm = kotlin.math.min(w, h)

        return TargetDetectionResult(
            centerX = (center.x / w).toFloat(),
            centerY = (center.y / h).toFloat(),
            radius = radius[0] / norm
        )
    }
}
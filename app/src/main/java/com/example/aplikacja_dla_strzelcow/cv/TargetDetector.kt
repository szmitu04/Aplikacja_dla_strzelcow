package com.example.aplikacja_dla_strzelcow.cv



import android.graphics.Bitmap
import com.example.aplikacja_dla_strzelcow.data.TargetParams
import org.opencv.android.OpenCVLoader // 👈 WAŻNE: Dodaj ten import
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import kotlin.math.min

//data class TargetDetectionResult(
//    val centerX: Float,
//    val centerY: Float,
//    val radius: Float
//)

object TargetDetector {
    init {
        // Ta linia ładuje natywną bibliotekę OpenCV.
        // Musi zostać wykonana raz, zanim użyjemy jakiejkolwiek funkcji OpenCV.
        OpenCVLoader.initDebug()
    }
    fun detect(bitmap: Bitmap): TargetParams? {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)
        val h = src.height()
        val w = src.width()
        val roiSize = min(h, w) / 2
        val roiX = (w - roiSize) / 2
        val roiY = (h - roiSize) / 2

        // Tworzymy prostokąt definiujący nasz ROI
        val roiRect = Rect(roiX, roiY, roiSize, roiSize)
        // Wycinamy ROI z oryginalnego obrazu `src`
        val srcRoi = Mat(src, roiRect)


        // --- ETAP 1: Detekcja tarczy (teraz działa na małym `srcRoi`) ---
        val gray = Mat()
        Imgproc.cvtColor(srcRoi, gray, Imgproc.COLOR_RGBA2GRAY) // Używamy srcRoi
        Imgproc.GaussianBlur(gray, gray, Size(9.0, 9.0), 2.0)

        val thresh = Mat()
        Imgproc.threshold(gray, thresh, 0.0, 255.0, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU)

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
        val centerInRoi = Point() // Środek znaleziony wewnątrz ROI
        val radiusArray = FloatArray(1)

        Imgproc.minEnclosingCircle(points, centerInRoi, radiusArray)

        // --- 🔴 NOWOŚĆ: Przeliczanie współrzędnych z ROI do pełnego obrazu 🔴 ---
        // Dodajemy przesunięcie (offset) ROI, aby uzyskać globalne koordynaty
        val globalCenterX = centerInRoi.x + roiX
        val globalCenterY = centerInRoi.y + roiY
        val radius = radiusArray[0] // Promień się nie zmienia

        // Normalizacja względem pełnego, oryginalnego obrazu
        val originalW = src.width().toFloat()
        val originalH = src.height().toFloat()
        val norm = min(originalW, originalH)

        return TargetParams(
            centerX = (globalCenterX / originalW).toFloat(),
            centerY = (globalCenterY / originalH).toFloat(),
            radius = radius / norm
        )
    }
}
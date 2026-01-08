package com.example.aplikacja_dla_strzelcow.cv


import android.graphics.*
import kotlin.math.min
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.example.aplikacja_dla_strzelcow.data.Shot
import com.example.aplikacja_dla_strzelcow.data.TargetParams
import kotlin.math.min
fun drawTargetOverlay(
    bitmap: Bitmap,
    target: TargetParams,
    shots: List<Shot>
): Bitmap {
    val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutable)

    val targetPaint = Paint().apply {
        color = Color.MAGENTA
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }

    val w = bitmap.width.toFloat()
    val h = bitmap.height.toFloat()
    val norm = min(w, h)
    val cxPx = target.centerX * w
    val cyPx = target.centerY * h
    val rPx = target.radius * norm
    canvas.drawCircle(cxPx, cyPx, rPx, targetPaint)
    val shotPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    // 🔴 NOWOŚĆ: Paint dla tekstu z numerem strzału 🔴
    val textPaint = Paint().apply {
        color = Color.CYAN
        textSize = 40f // Możesz dostosować rozmiar
        style = Paint.Style.FILL
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    val textBounds = Rect()

    // Używamy `forEachIndexed` aby mieć dostęp do indeksu (numeru strzału)
    shots.forEachIndexed { index, shot ->
        val shotX_px = cxPx + shot.x * rPx
        val shotY_px = cyPx + shot.y * rPx

        // Rysujemy ramkę (tak jak wcześniej)
        canvas.drawRect(shotX_px - 20f, shotY_px - 20f, shotX_px + 20f, shotY_px + 20f, shotPaint)

        // 🔴 NOWOŚĆ: Rysujemy numer strzału (indeks + 1) 🔴
        val shotNumber = (index + 1).toString()
        // Mierzymy granice tekstu, aby precyzyjnie go wyśrodkować
        textPaint.getTextBounds(shotNumber, 0, shotNumber.length, textBounds)
        // Rysujemy numer poniżej ramki
        canvas.drawText(shotNumber, shotX_px, shotY_px + 25f + textBounds.height(), textPaint)
    }

    return mutable
}
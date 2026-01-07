package com.example.aplikacja_dla_strzelcow.cv


import android.graphics.*
import kotlin.math.min

fun drawTargetOverlay(
    bitmap: Bitmap,
    centerX: Float,
    centerY: Float,
    radius: Float
): Bitmap {
    val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutable)

    val paint = Paint().apply {
        color = Color.MAGENTA
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    val w = bitmap.width.toFloat()
    val h = bitmap.height.toFloat()
    val norm = min(w, h)

    val cxPx = centerX * w
    val cyPx = centerY * h
    val rPx = radius * norm

    canvas.drawCircle(cxPx, cyPx, rPx, paint)

    return mutable
}
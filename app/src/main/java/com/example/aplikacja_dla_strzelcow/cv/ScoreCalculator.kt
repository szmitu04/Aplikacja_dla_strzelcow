package com.example.aplikacja_dla_strzelcow.cv


import kotlin.math.pow
import kotlin.math.sqrt

object ScoreCalculator {

    // Przyjmuje znormalizowane koordynaty strzału (x, y)
    // gdzie (0,0) to środek, a promień tarczy to 1.0
    fun calculate(x: Float, y: Float): Int {
        // TODO: Zaimplementować prawdziwą logikę punktacji
        // Na razie zwracamy 0 jako placeholder.

        // Przykładowa przyszła logika:
        // val distance = sqrt(x.pow(2) + y.pow(2))
        // return when {
        //     distance <= 0.1f -> 10 // 10% promienia
        //     distance <= 0.2f -> 9  // 20% promienia
        //     // ... itd.
        //     else -> 0
        // }
        return 0
    }
}
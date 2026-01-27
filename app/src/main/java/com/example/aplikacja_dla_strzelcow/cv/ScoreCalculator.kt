package com.example.aplikacja_dla_strzelcow.cv


import kotlin.math.pow
import kotlin.math.sqrt



object ScoreCalculator {
    // Rzeczywiste wymiary w milimetrach
    private const val TARGET_1_RING_RADIUS_MM = 65.0f
    private const val SHOT_RADIUS_MM = 2.25f
    // Granice pól w milimetrach (liczone od środka)
    private const val R_10_MM = 2.0f
    private const val R_9_MM = 2.0f + 7.0f
    private const val R_8_MM = 2.0f + 14.0f
    private const val R_7_MM = 2.0f + 21.0f
    private const val R_6_MM = 2.0f + 28.0f
    private const val R_5_MM = 2.0f + 35.0f
    private const val R_4_MM = 2.0f + 42.0f
    private const val R_3_MM = 2.0f + 49.0f
    private const val R_2_MM = 2.0f + 56.0f
    private const val R_1_MM = 2.0f + 63.0f

//    // Definiujemy granice pól punktowych w naszej względnej skali (promień_pola / 65mm)
//    private const val R_10 = 2.0f / 65.0f   // ~0.0307
//    private const val R_9 = (2.0f + 7.0f) / 65.0f   // ~0.138
//    private const val R_8 = (2.0f + 14.0f) / 65.0f  // ~0.246
//    private const val R_7 = (2.0f + 21.0f) / 65.0f  // ~0.353
//    private const val R_6 = (2.0f + 28.0f) / 65.0f  // ~0.461
//    private const val R_5 = (2.0f + 35.0f) / 65.0f  // ~0.569
//    private const val R_4 = (2.0f + 42.0f) / 65.0f  // ~0.676
//    private const val R_3 = (2.0f + 49.0f) / 65.0f  // ~0.784
//    private const val R_2 = (2.0f + 56.0f) / 65.0f  // ~0.892
//    private const val R_1 = (2.0f + 63.0f) / 65.0f  // ~1.0 (powinno być równe promieniowi czarnego pola)


    /**
     * Oblicza punkty dla strzału, uwzględniając jego promień.
     * @param shotX_px Współrzędna X środka strzału w pikselach.
     * @param shotY_px Współrzędna Y środka strzału w pikselach.
     * @param targetCenterX_px Współrzędna X środka tarczy w pikselach.
     * @param targetCenterY_px Współrzędna Y środka tarczy w pikselach.
     * @param target1RingRadiusPx Promień pierścienia "1" (całej tarczy) w pikselach.
     * @return Wartość punktowa od 0 do 10.
     */
    fun calculate(
        shotX_px: Float,
        shotY_px: Float,
        targetCenterX_px: Float,
        targetCenterY_px: Float,
        target1RingRadiusPx: Float
    ): Int {
        // 1. Obliczamy, ile pikseli odpowiada jednemu milimetrowi na tarczy
        val pixelsPerMm = target1RingRadiusPx / TARGET_1_RING_RADIUS_MM

        // 2. Obliczamy promień przestrzeliny w pikselach
        val shotRadiusPx = SHOT_RADIUS_MM * pixelsPerMm

        // 3. Obliczamy odległość środka strzału od środka tarczy w pikselach
        val distanceInPixels = sqrt(
            (shotX_px - targetCenterX_px).pow(2) + (shotY_px - targetCenterY_px).pow(2)
        )

        // 4. Obliczamy odległość krawędzi strzału od środka tarczy w pikselach
        val edgeDistanceInPixels = distanceInPixels - shotRadiusPx

        // 5. Sprawdzamy, w którym polu punktowym mieści się krawędź strzału,
        // porównując odległość w pikselach z granicami pól przeliczonymi na piksele.
        return when {
            edgeDistanceInPixels <= R_10_MM * pixelsPerMm -> 10
            edgeDistanceInPixels <= R_9_MM * pixelsPerMm -> 9
            edgeDistanceInPixels <= R_8_MM * pixelsPerMm -> 8
            edgeDistanceInPixels <= R_7_MM * pixelsPerMm -> 7
            edgeDistanceInPixels <= R_6_MM * pixelsPerMm -> 6
            edgeDistanceInPixels <= R_5_MM * pixelsPerMm -> 5
            edgeDistanceInPixels <= R_4_MM * pixelsPerMm -> 4
            edgeDistanceInPixels <= R_3_MM * pixelsPerMm -> 3
            edgeDistanceInPixels <= R_2_MM * pixelsPerMm -> 2
            edgeDistanceInPixels <= R_1_MM * pixelsPerMm -> 1
            else -> 0
        }
    }
}

//    // Definiujemy względny promień samej przestrzeliny
//    private const val SHOT_RADIUS = 2.25f / 65.0f   // ~0.0346
//
//    /**
//     * Oblicza punkty dla strzału, uwzględniając jego promień (zasada "zahaczania").
//     * @param x Względna współrzędna X środka strzału.
//     * @param y Względna współrzędna Y środka strzału.
//     * @return Wartość punktowa od 0 do 10.
//     */
//    fun calculate(x: Float, y: Float): Int {
//        // 1. Obliczamy odległość środka strzału od środka tarczy (0,0)
//        val centerDistance = sqrt(x.pow(2) + y.pow(2))
//
//        // 2. Obliczamy odległość krawędzi strzału najbliższej środka tarczy.
//        // To jest klucz do zasady "zahaczania".
//        val edgeDistance = centerDistance - SHOT_RADIUS
//
//        // 3. Sprawdzamy, w którym polu punktowym mieści się krawędź strzału.
//        // Zaczynamy od najwyższej wartości.
//        return when {
//            edgeDistance <= R_10 -> 10
//            edgeDistance <= R_9 -> 9
//            edgeDistance <= R_8 -> 8
//            edgeDistance <= R_7 -> 7
//            edgeDistance <= R_6 -> 6
//            edgeDistance <= R_5 -> 5
//            edgeDistance <= R_4 -> 4
//            edgeDistance <= R_3 -> 3
//            edgeDistance <= R_2 -> 2
//            edgeDistance <= R_1 -> 1
//            else -> 0 // Strzał poza punktowanym obszarem
//        }
//    }
//}

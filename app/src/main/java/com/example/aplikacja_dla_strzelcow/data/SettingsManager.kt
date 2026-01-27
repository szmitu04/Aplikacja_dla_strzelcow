package com.example.aplikacja_dla_strzelcow.data


import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_THRESHOLD = "detection_threshold"
        const val DEFAULT_THRESHOLD = 20 // Domyślna wartość progu
    }

    // Funkcja do zapisywania progu
    fun saveThreshold(threshold: Int) {
        prefs.edit { putInt(KEY_THRESHOLD, threshold) }
        //prefs.edit().putInt(KEY_THRESHOLD, threshold).apply()
    }

    // Funkcja do odczytywania progu
    fun getThreshold(): Int {
        return prefs.getInt(KEY_THRESHOLD, DEFAULT_THRESHOLD)
    }
}
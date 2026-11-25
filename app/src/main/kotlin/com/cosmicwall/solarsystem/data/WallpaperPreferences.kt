package com.cosmicwall.solarsystem.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class WallpaperPreferences(context: Context) {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    var selectedPlanet: String
        get() = prefs.getString("selected_planet", "Earth") ?: "Earth"
        set(value) {
            prefs.edit().putString("selected_planet", value).apply()
        }

    val unlockZoomEffect: Boolean get() = prefs.getBoolean("unlock_zoom_effect", true)
    val quality: String get() = prefs.getString("quality", "high") ?: "high"
    val orbitSpeedMultiplier: Float get() = prefs.getInt("orbit_speed", 50) / 50f
    val autoRotate: Boolean get() = prefs.getBoolean("auto_rotate", true)
    val showLabels: Boolean get() = prefs.getBoolean("planet_labels", true)
    val starDensity: Float get() = prefs.getInt("star_density", 70).toFloat()

    val sphereSegmentsLat: Int get() = when (quality) {
        "low" -> 24
        "medium" -> 48
        else -> 64
    }

    val sphereSegmentsLon: Int get() = when (quality) {
        "low" -> 48
        "medium" -> 96
        else -> 128
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}

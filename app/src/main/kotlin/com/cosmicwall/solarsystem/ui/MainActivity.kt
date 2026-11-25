package com.cosmicwall.solarsystem.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cosmicwall.solarsystem.data.WallpaperPreferences
import com.cosmicwall.solarsystem.databinding.ActivityMainBinding
import com.cosmicwall.solarsystem.interaction.CameraController
import com.cosmicwall.solarsystem.interaction.RayCaster
import com.cosmicwall.solarsystem.interaction.TouchHandler
import com.cosmicwall.solarsystem.renderer.SolarSystemRenderer
import com.cosmicwall.solarsystem.wallpaper.SolarSystemWallpaperService
import com.google.android.material.chip.Chip

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var renderer: SolarSystemRenderer? = null
    private var touchHandler: TouchHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup live 3D preview directly in the app
        val camera = CameraController()
        val rayCaster = RayCaster()
        val ren = SolarSystemRenderer(this, camera, rayCaster)
        renderer = ren

        touchHandler = TouchHandler(this, camera, rayCaster).apply {
            planets = ren.planets
            onPlanetTapped = { planet ->
                renderer?.showPlanetLabel(planet)
                Toast.makeText(this@MainActivity, "🪐 ${planet.config.name} selected (Double-tap to reset view)", Toast.LENGTH_SHORT).show()
            }
        }

        binding.glPreviewView.apply {
            setEGLContextClientVersion(3)
            setEGLConfigChooser(8, 8, 8, 8, 24, 0)
            setPreserveEGLContextOnPause(true)
            setRenderer(ren)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            setOnTouchListener { v, event ->
                v.performClick()
                touchHandler?.onTouchEvent(event) ?: false
            }
        }

        binding.btnSetWallpaper.setOnClickListener {
            try {
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                    putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(this@MainActivity, SolarSystemWallpaperService::class.java)
                    )
                }
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback for devices where ACTION_CHANGE_LIVE_WALLPAPER needs standard chooser
                try {
                    startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
                } catch (e2: Exception) {
                    Toast.makeText(this@MainActivity, "Please select Solar System in your device Wallpaper settings", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        setupPlanetSelector(ren)
    }

    private fun setupPlanetSelector(ren: SolarSystemRenderer) {
        val prefs = WallpaperPreferences(this)
        val currentSelection = prefs.selectedPlanet

        val options = listOf(
            "all" to "🌌 Solar System",
            "Earth" to "🌍 Earth",
            "Saturn" to "🪐 Saturn",
            "Mars" to "🔴 Mars",
            "Jupiter" to "🟠 Jupiter",
            "Sun" to "☀️ Sun",
            "Venus" to "🟡 Venus",
            "Mercury" to "🪨 Mercury",
            "Uranus" to "🩵 Uranus",
            "Neptune" to "🔵 Neptune"
        )

        val chipBgStates = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(Color.parseColor("#FF5C6BC0"), Color.parseColor("#33FFFFFF"))
        )
        val chipTextStates = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(Color.WHITE, Color.parseColor("#DDFFFFFF"))
        )

        binding.chipGroupPlanets.removeAllViews()
        for ((id, label) in options) {
            val chip = Chip(this).apply {
                tag = id
                text = label
                isCheckable = true
                isChecked = id.equals(currentSelection, ignoreCase = true)
                chipBackgroundColor = chipBgStates
                setTextColor(chipTextStates)
                chipStrokeWidth = 0f
                setOnClickListener {
                    ren.selectPlanet(id)
                    ren.onWallpaperUnlocked()
                }
            }
            binding.chipGroupPlanets.addView(chip)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let { touchHandler?.onTouchEvent(it) }
        return super.onTouchEvent(event)
    }

    override fun onResume() {
        super.onResume()
        val current = WallpaperPreferences(this).selectedPlanet
        for (i in 0 until binding.chipGroupPlanets.childCount) {
            val chip = binding.chipGroupPlanets.getChildAt(i) as? Chip ?: continue
            if ((chip.tag as? String).equals(current, ignoreCase = true) && !chip.isChecked) {
                chip.isChecked = true
            }
        }
        binding.glPreviewView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.glPreviewView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.glPreviewView.queueEvent {
            renderer?.release()
        }
    }
}

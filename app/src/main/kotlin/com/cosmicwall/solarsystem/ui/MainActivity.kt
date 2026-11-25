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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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

    private val planetMetadata = mapOf(
        "all" to ("Solar System" to "8 Planets • Heliocentric 3D Orbit"),
        "Earth" to ("Earth" to "3rd Planet • 149.6M km • 24h Day"),
        "Saturn" to ("Saturn" to "6th Planet • 1.43B km • Cassini Rings"),
        "Mars" to ("Mars" to "4th Planet • 227.9M km • Red Planet"),
        "Jupiter" to ("Jupiter" to "5th Planet • 778.5M km • Gas Giant"),
        "Sun" to ("The Sun" to "G-Type Star • 5,778 K Photosphere"),
        "Venus" to ("Venus" to "2nd Planet • 108.2M km • Dense Cloud"),
        "Mercury" to ("Mercury" to "1st Planet • 57.9M km • Cratered World"),
        "Uranus" to ("Uranus" to "7th Planet • 2.87B km • Ice Giant"),
        "Neptune" to ("Neptune" to "8th Planet • 4.50B km • Supersonic Winds")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyEdgeToEdgeInsets()

        val camera = CameraController()
        val rayCaster = RayCaster()
        val ren = SolarSystemRenderer(this, camera, rayCaster)
        renderer = ren

        touchHandler = TouchHandler(this, camera, rayCaster).apply {
            planets = ren.planets
            onPlanetTapped = { planet ->
                renderer?.showPlanetLabel(planet)
                updateHeader(planet.config.name)
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
                try {
                    startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
                } catch (e2: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        "Please select Solar System in your device Wallpaper settings",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnReplayZoom.setOnClickListener {
            ren.onWallpaperUnlocked()
        }

        setupPlanetSelector(ren)
    }

    private fun applyEdgeToEdgeInsets() {
        val topBase = resources.displayMetrics.density * 16
        val bottomBase = resources.displayMetrics.density * 24

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.topBar.updatePadding(top = sysBars.top + topBase.toInt())
            binding.bottomBar.updatePadding(bottom = sysBars.bottom + bottomBase.toInt())
            insets
        }
    }

    private fun updateHeader(planetId: String) {
        val (title, subtitle) = planetMetadata[planetId] ?: (planetId to "Interactive 3D Planet")
        binding.tvPlanetTitle.text = title
        binding.tvPlanetSubtitle.text = subtitle
    }

    private fun setupPlanetSelector(ren: SolarSystemRenderer) {
        val currentSelection = WallpaperPreferences(this).selectedPlanet
        updateHeader(currentSelection)

        val options = listOf(
            "all" to "Solar System",
            "Earth" to "Earth",
            "Saturn" to "Saturn",
            "Mars" to "Mars",
            "Jupiter" to "Jupiter",
            "Sun" to "Sun",
            "Venus" to "Venus",
            "Mercury" to "Mercury",
            "Uranus" to "Uranus",
            "Neptune" to "Neptune"
        )

        val chipBgStates = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(Color.parseColor("#FF4A66F5"), Color.parseColor("#1FFFFFFF"))
        )
        val chipStrokeStates = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(Color.parseColor("#FF8FA8FF"), Color.parseColor("#33FFFFFF"))
        )
        val chipTextStates = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(Color.WHITE, Color.parseColor("#CCFFFFFF"))
        )

        val density = resources.displayMetrics.density
        binding.chipGroupPlanets.removeAllViews()
        for ((id, label) in options) {
            val chip = Chip(this).apply {
                tag = id
                text = label
                isCheckable = true
                isCheckedIconVisible = false
                isChecked = id.equals(currentSelection, ignoreCase = true)
                chipBackgroundColor = chipBgStates
                chipStrokeColor = chipStrokeStates
                chipStrokeWidth = density * 1f
                chipCornerRadius = density * 20f
                setTextColor(chipTextStates)
                textSize = 13f
                setOnClickListener {
                    updateHeader(id)
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
        updateHeader(current)
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

package com.cosmicwall.solarsystem.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.cosmicwall.solarsystem.R
import com.cosmicwall.solarsystem.data.WallpaperPreferences
import com.cosmicwall.solarsystem.databinding.ActivityMainBinding
import com.cosmicwall.solarsystem.gl.CosmicEGLConfigChooser
import com.cosmicwall.solarsystem.interaction.CameraController
import com.cosmicwall.solarsystem.interaction.RayCaster
import com.cosmicwall.solarsystem.interaction.TouchHandler
import com.cosmicwall.solarsystem.renderer.SolarSystemRenderer
import com.cosmicwall.solarsystem.wallpaper.SolarSystemWallpaperService

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var renderer: SolarSystemRenderer? = null
    private var touchHandler: TouchHandler? = null
    private val planetCardViews = mutableMapOf<String, LinearLayout>()

    private data class PlanetOption(
        val id: String,
        val label: String,
        val subtitle: String,
        val specs: String,
        val highlightColor: Int,
        val midColor: Int,
        val shadowColor: Int,
        val rimColor: Int
    )

    private val options = listOf(
        PlanetOption("Earth", "Earth", "3rd Planet • 149.6M km • 24h Day", "Diameter 12,742 km  •  15 °C  •  1 Moon",
            Color.parseColor("#FF7CD4FF"), Color.parseColor("#FF1F6FEB"), Color.parseColor("#FF081D4D"), Color.parseColor("#FF58B8FF")),
        PlanetOption("Saturn", "Saturn", "6th Planet • 1.43B km • Cassini Rings", "Diameter 116,460 km  •  -140 °C  •  146 Moons",
            Color.parseColor("#FFFCE8B2"), Color.parseColor("#FFD2A860"), Color.parseColor("#FF5C421C"), Color.parseColor("#FFF0D290")),
        PlanetOption("Mars", "Mars", "4th Planet • 227.9M km • Red Planet", "Diameter 6,779 km  •  -65 °C  •  2 Moons",
            Color.parseColor("#FFFF9A6C"), Color.parseColor("#FFC74A24"), Color.parseColor("#FF4A1408"), Color.parseColor("#FFE87042")),
        PlanetOption("Jupiter", "Jupiter", "5th Planet • 778.5M km • Gas Giant", "Diameter 139,820 km  •  -110 °C  •  95 Moons",
            Color.parseColor("#FFF5D3A8"), Color.parseColor("#FFC47A45"), Color.parseColor("#FF4D2712"), Color.parseColor("#FFE6A772")),
        PlanetOption("all", "Solar System", "8 Planets • Heliocentric 3D Orbit", "1 Star  •  8 Planets  •  Orbit Tracks",
            Color.parseColor("#FF9CB6FF"), Color.parseColor("#FF4562D9"), Color.parseColor("#FF101840"), Color.parseColor("#FF7A9AFF")),
        PlanetOption("Sun", "Sun", "G-Type Star • 5,778 K Photosphere", "Diameter 1.39M km  •  4.6B Years Old",
            Color.parseColor("#FFFFF3B0"), Color.parseColor("#FFFF8C1A"), Color.parseColor("#FF991B00"), Color.parseColor("#FFFFB830")),
        PlanetOption("Venus", "Venus", "2nd Planet • 108.2M km • Cloud Deck", "Diameter 12,104 km  •  464 °C  •  0 Moons",
            Color.parseColor("#FFFFF0C8"), Color.parseColor("#FFD9AE6E"), Color.parseColor("#FF63471E"), Color.parseColor("#FFF2D08A")),
        PlanetOption("Mercury", "Mercury", "1st Planet • 57.9M km • Cratered", "Diameter 4,879 km  •  167 °C  •  0 Moons",
            Color.parseColor("#FFE0DFDC"), Color.parseColor("#FF8F8D88"), Color.parseColor("#FF2E2D2B"), Color.parseColor("#FFB5B3AE")),
        PlanetOption("Uranus", "Uranus", "7th Planet • 2.87B km • Ice Giant", "Diameter 50,724 km  •  -195 °C  •  28 Moons",
            Color.parseColor("#FFD8FAFF"), Color.parseColor("#FF6FC7D6"), Color.parseColor("#FF1A5260"), Color.parseColor("#FF9DE8F2")),
        PlanetOption("Neptune", "Neptune", "8th Planet • 4.50B km • Deep Azure", "Diameter 49,244 km  •  -200 °C  •  16 Moons",
            Color.parseColor("#FF82ADFF"), Color.parseColor("#FF2D5CF2"), Color.parseColor("#FF0B1C63"), Color.parseColor("#FF5888FF"))
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
                updateSelectionUI(planet.config.name)
            }
        }

        binding.glPreviewView.apply {
            setEGLContextClientVersion(3)
            setEGLConfigChooser(CosmicEGLConfigChooser())
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

        setupPlanetCards(ren)
    }

    private fun applyEdgeToEdgeInsets() {
        val density = resources.displayMetrics.density
        val topBase = (density * 16).toInt()
        val bottomBase = (density * 24).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.topBar.updatePadding(top = sysBars.top + topBase)
            binding.bottomBar.updatePadding(bottom = sysBars.bottom + bottomBase)
            insets
        }
    }

    private fun updateSelectionUI(selectedId: String) {
        val meta = options.find { it.id.equals(selectedId, ignoreCase = true) }
        binding.tvPlanetTitle.text = meta?.label ?: selectedId
        binding.tvPlanetSubtitle.text = meta?.subtitle ?: "Interactive 3D Planet"
        binding.tvPlanetSpecs.text = meta?.specs ?: "Orbital View"

        val density = resources.displayMetrics.density
        binding.tvPlanetSpecs.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = density * 12f
            setColor(Color.parseColor("#1FFFFFFF"))
            setStroke((density * 1f).toInt().coerceAtLeast(1), Color.parseColor("#2EFFFFFF"))
        }
        for ((id, card) in planetCardViews) {
            val isSelected = id.equals(selectedId, ignoreCase = true)
            val cardBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = density * 16f
                if (isSelected) {
                    setColor(Color.parseColor("#335C7CFA"))
                    setStroke((density * 1.5f).toInt().coerceAtLeast(1), Color.parseColor("#CCF0F4FF"))
                } else {
                    setColor(Color.parseColor("#14FFFFFF"))
                    setStroke((density * 1f).toInt().coerceAtLeast(1), Color.parseColor("#1FFFFFFF"))
                }
            }
            card.background = cardBg
            card.findViewById<TextView>(R.id.tvPlanetName)?.setTextColor(
                if (isSelected) Color.WHITE else Color.parseColor("#AAFFFFFF")
            )
        }
    }

    private fun setupPlanetCards(ren: SolarSystemRenderer) {
        val density = resources.displayMetrics.density
        val inflater = LayoutInflater.from(this)
        binding.planetCardContainer.removeAllViews()
        planetCardViews.clear()

        for (opt in options) {
            val card = inflater.inflate(R.layout.item_planet_option, binding.planetCardContainer, false) as LinearLayout
            val orbView = card.findViewById<View>(R.id.viewPlanetOrb)
            val nameView = card.findViewById<TextView>(R.id.tvPlanetName)

            nameView.text = opt.label

            // Crisp 3D lit orb preview badge
            val orbDrawable = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(opt.highlightColor, opt.midColor, opt.shadowColor)
            ).apply {
                shape = GradientDrawable.OVAL
                setStroke((density * 1.5f).toInt().coerceAtLeast(1), opt.rimColor)
            }
            orbView.background = orbDrawable

            card.setOnClickListener {
                updateSelectionUI(opt.id)
                ren.selectPlanet(opt.id)
            }

            planetCardViews[opt.id] = card
            binding.planetCardContainer.addView(card)
        }

        val currentSelection = WallpaperPreferences(this).selectedPlanet
        updateSelectionUI(currentSelection)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let { touchHandler?.onTouchEvent(it) }
        return super.onTouchEvent(event)
    }

    override fun onResume() {
        super.onResume()
        val current = WallpaperPreferences(this).selectedPlanet
        updateSelectionUI(current)
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

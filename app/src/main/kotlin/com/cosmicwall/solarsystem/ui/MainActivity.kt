package com.cosmicwall.solarsystem.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cosmicwall.solarsystem.R
import com.cosmicwall.solarsystem.databinding.ActivityMainBinding
import com.cosmicwall.solarsystem.wallpaper.SolarSystemWallpaperService

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                Toast.makeText(this, "Unable to open wallpaper picker", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}

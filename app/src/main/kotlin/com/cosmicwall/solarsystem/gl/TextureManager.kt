package com.cosmicwall.solarsystem.gl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.opengl.GLES32
import android.opengl.GLUtils
import android.util.Log

class TextureManager(private val context: Context) {
    private val textures = mutableMapOf<String, Int>()

    fun loadTexture(name: String, assetPath: String): Int {
        val textureIds = IntArray(1)
        GLES32.glGenTextures(1, textureIds, 0)
        val textureId = textureIds[0]

        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, textureId)
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR_MIPMAP_LINEAR)
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR)
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_S, GLES32.GL_REPEAT)
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_T, GLES32.GL_CLAMP_TO_EDGE)

        var bitmap: Bitmap? = null
        try {
            val options = BitmapFactory.Options().apply {
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            bitmap = context.assets.open(assetPath).use {
                BitmapFactory.decodeStream(it, null, options)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Asset not found ($assetPath), generating fallback procedural texture for $name")
        }

        if (bitmap == null) {
            bitmap = createFallbackBitmap(name)
        }

        GLUtils.texImage2D(GLES32.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES32.glGenerateMipmap(GLES32.GL_TEXTURE_2D)
        bitmap.recycle()
        textures[name] = textureId
        Log.d(TAG, "Ready texture: $name (id=$textureId)")

        return textureId
    }

    private fun createFallbackBitmap(name: String): Bitmap {
        val width = 256
        val height = 128
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val baseColor = when {
            name.contains("sun") -> Color.rgb(255, 180, 50)
            name.contains("mercury") -> Color.rgb(160, 160, 160)
            name.contains("venus") -> Color.rgb(230, 200, 150)
            name.contains("earth_night") -> Color.rgb(10, 10, 20)
            name.contains("earth_normal") -> Color.rgb(128, 128, 255)
            name.contains("earth_clouds") -> Color.argb(180, 255, 255, 255)
            name.contains("earth") -> Color.rgb(30, 100, 200)
            name.contains("mars") -> Color.rgb(200, 80, 40)
            name.contains("jupiter") -> Color.rgb(210, 160, 120)
            name.contains("saturn_ring") -> Color.argb(200, 200, 180, 140)
            name.contains("saturn") -> Color.rgb(230, 200, 140)
            name.contains("uranus") -> Color.rgb(140, 210, 230)
            name.contains("neptune") -> Color.rgb(60, 100, 230)
            else -> Color.GRAY
        }

        canvas.drawColor(baseColor)

        if (name.contains("jupiter") || name.contains("saturn")) {
            val bandPaint = Paint().apply {
                color = Color.rgb(180, 120, 80)
                strokeWidth = 8f
            }
            for (y in 0 until height step 16) {
                canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), bandPaint)
            }
        }

        return bmp
    }

    fun get(name: String): Int = textures[name] ?: 0

    fun bind(name: String, unit: Int) {
        val textureId = textures[name] ?: return
        GLES32.glActiveTexture(GLES32.GL_TEXTURE0 + unit)
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, textureId)
    }

    fun loadAll() {
        loadTexture("sun_diffuse", "textures/sun.jpg")
        loadTexture("mercury_diffuse", "textures/mercury.jpg")
        loadTexture("venus_diffuse", "textures/venus_atmosphere.jpg")
        loadTexture("earth_diffuse", "textures/earth_daymap.jpg")
        loadTexture("earth_normal", "textures/earth_normal.jpg")
        loadTexture("earth_night", "textures/earth_nightmap.jpg")
        loadTexture("earth_clouds", "textures/earth_clouds.jpg")
        loadTexture("mars_diffuse", "textures/mars.jpg")
        loadTexture("jupiter_diffuse", "textures/jupiter.jpg")
        loadTexture("saturn_diffuse", "textures/saturn.jpg")
        loadTexture("saturn_ring", "textures/saturn_ring.png")
        loadTexture("uranus_diffuse", "textures/uranus.jpg")
        loadTexture("neptune_diffuse", "textures/neptune.jpg")
    }

    fun releaseAll() {
        val ids = textures.values.toIntArray()
        if (ids.isNotEmpty()) {
            GLES32.glDeleteTextures(ids.size, ids, 0)
        }
        textures.clear()
    }

    companion object {
        private const val TAG = "TextureManager"
    }
}

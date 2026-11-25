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
        val width = 512
        val height = 256
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when {
            name.contains("sun") -> {
                canvas.drawColor(Color.rgb(255, 120, 20))
                paint.color = Color.rgb(255, 220, 90)
                for (y in 0 until height step 4) {
                    val bandWidth = (Math.sin(y * 0.1) * 20 + 30).toFloat()
                    paint.strokeWidth = bandWidth
                    canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
                }
            }
            name.contains("mercury") -> {
                canvas.drawColor(Color.rgb(150, 150, 150))
                paint.color = Color.rgb(110, 110, 110)
                for (i in 0 until 40) {
                    val cx = (Math.sin(i * 1.7) * 0.5 + 0.5).toFloat() * width
                    val cy = (Math.cos(i * 2.3) * 0.5 + 0.5).toFloat() * height
                    val r = (i % 7 + 4).toFloat() * 2f
                    canvas.drawCircle(cx, cy, r, paint)
                }
            }
            name.contains("venus") -> {
                canvas.drawColor(Color.rgb(230, 205, 160))
                paint.color = Color.rgb(215, 185, 135)
                paint.strokeWidth = 14f
                for (y in 0 until height step 24) {
                    canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
                }
            }
            name.contains("earth_diffuse") -> {
                // Ocean
                canvas.drawColor(Color.rgb(24, 75, 140))
                // Landmasses
                paint.color = Color.rgb(55, 125, 60)
                val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(65, 135, 70) }
                // Continents approximation
                canvas.drawOval(width * 0.15f, height * 0.25f, width * 0.35f, height * 0.65f, rectPaint)
                canvas.drawOval(width * 0.45f, height * 0.20f, width * 0.75f, height * 0.70f, rectPaint)
                canvas.drawOval(width * 0.78f, height * 0.55f, width * 0.92f, height * 0.75f, rectPaint)
                // Polar ice caps
                paint.color = Color.rgb(240, 248, 255)
                canvas.drawRect(0f, 0f, width.toFloat(), height * 0.12f, paint)
                canvas.drawRect(0f, height * 0.88f, width.toFloat(), height.toFloat(), paint)
            }
            name.contains("earth_night") -> {
                canvas.drawColor(Color.rgb(4, 6, 12))
                paint.color = Color.rgb(255, 215, 90)
                for (i in 0 until 120) {
                    val cx = (Math.sin(i * 3.1) * 0.35 + 0.55).toFloat() * width
                    val cy = (Math.cos(i * 2.7) * 0.25 + 0.45).toFloat() * height
                    canvas.drawCircle(cx, cy, 2f, paint)
                }
            }
            name.contains("earth_clouds") -> {
                canvas.drawColor(Color.TRANSPARENT)
                paint.color = Color.argb(160, 255, 255, 255)
                for (i in 0 until 18) {
                    val cx = (i * 31f) % width
                    val cy = (height * 0.15f) + (i * 19f) % (height * 0.7f)
                    canvas.drawOval(cx, cy, cx + 120f, cy + 30f, paint)
                }
            }
            name.contains("earth_normal") -> {
                canvas.drawColor(Color.rgb(128, 128, 255))
            }
            name.contains("mars") -> {
                canvas.drawColor(Color.rgb(195, 80, 42))
                // Dark basalt plains
                paint.color = Color.rgb(130, 48, 26)
                canvas.drawOval(width * 0.3f, height * 0.35f, width * 0.65f, height * 0.65f, paint)
                // Polar ice caps
                paint.color = Color.WHITE
                canvas.drawRect(0f, 0f, width.toFloat(), height * 0.08f, paint)
                canvas.drawRect(0f, height * 0.92f, width.toFloat(), height.toFloat(), paint)
            }
            name.contains("jupiter") -> {
                canvas.drawColor(Color.rgb(215, 175, 135))
                val bandColors = intArrayOf(
                    Color.rgb(190, 130, 80), Color.rgb(235, 215, 185),
                    Color.rgb(160, 95, 55), Color.rgb(240, 225, 200),
                    Color.rgb(180, 115, 70), Color.rgb(210, 160, 115)
                )
                for (i in bandColors.indices) {
                    paint.color = bandColors[i]
                    val y = i * (height / bandColors.size).toFloat()
                    val h = (height / bandColors.size).toFloat()
                    canvas.drawRect(0f, y, width.toFloat(), y + h, paint)
                }
                // Great Red Spot
                paint.color = Color.rgb(195, 60, 40)
                canvas.drawOval(width * 0.6f, height * 0.58f, width * 0.74f, height * 0.72f, paint)
            }
            name.contains("saturn_ring") -> {
                canvas.drawColor(Color.TRANSPARENT)
                // Outer Ring A
                paint.color = Color.argb(180, 205, 185, 145)
                canvas.drawRect(width * 0.78f, 0f, width.toFloat(), height.toFloat(), paint)
                // Cassini division gap (transparent)
                // Ring B (Bright dense inner ring)
                paint.color = Color.argb(230, 230, 210, 170)
                canvas.drawRect(width * 0.25f, 0f, width * 0.72f, height.toFloat(), paint)
                // Ring C (Faint crepe ring)
                paint.color = Color.argb(80, 160, 140, 110)
                canvas.drawRect(0f, 0f, width * 0.25f, height.toFloat(), paint)
            }
            name.contains("saturn") -> {
                canvas.drawColor(Color.rgb(225, 198, 142))
                paint.color = Color.rgb(195, 165, 110)
                paint.strokeWidth = 10f
                for (y in 0 until height step 20) {
                    canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
                }
            }
            name.contains("uranus") -> {
                canvas.drawColor(Color.rgb(155, 218, 232))
            }
            name.contains("neptune") -> {
                canvas.drawColor(Color.rgb(55, 95, 225))
                paint.color = Color.rgb(35, 65, 180)
                canvas.drawRect(0f, height * 0.4f, width.toFloat(), height * 0.55f, paint)
                paint.color = Color.argb(160, 255, 255, 255)
                canvas.drawOval(width * 0.35f, height * 0.44f, width * 0.55f, height * 0.48f, paint)
            }
            else -> {
                canvas.drawColor(Color.GRAY)
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

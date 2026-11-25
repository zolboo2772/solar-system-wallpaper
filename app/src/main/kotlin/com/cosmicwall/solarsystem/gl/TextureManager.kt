package com.cosmicwall.solarsystem.gl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
        return ProceduralTextureGenerator.generate(name)
    }

    fun get(name: String): Int = textures[name] ?: 0

    fun bind(name: String, unit: Int) {
        val textureId = textures[name] ?: return
        GLES32.glActiveTexture(GLES32.GL_TEXTURE0 + unit)
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, textureId)
    }

    fun loadAll() {
        loadTexture("sun_diffuse", "textures/sun.jpg")
        loadTexture("moon_diffuse", "textures/moon.jpg")
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

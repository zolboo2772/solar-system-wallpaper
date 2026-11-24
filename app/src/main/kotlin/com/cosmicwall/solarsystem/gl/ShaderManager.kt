package com.cosmicwall.solarsystem.gl

import android.content.Context
import android.util.Log

class ShaderManager(private val context: Context) {
    private val programs = mutableMapOf<String, ShaderProgram>()

    fun loadShader(name: String, vertexFile: String, fragmentFile: String): ShaderProgram? {
        val vertSrc = loadAsset(vertexFile) ?: return null
        val fragSrc = loadAsset(fragmentFile) ?: return null
        val program = ShaderProgram(vertSrc, fragSrc)
        if (!program.compile()) {
            Log.e(TAG, "Failed to compile shader: $name")
            return null
        }
        programs[name] = program
        return program
    }

    fun get(name: String): ShaderProgram? = programs[name]

    fun compileAll() {
        loadShader("planet", "shaders/planet.vert", "shaders/planet.frag")
        loadShader("sun", "shaders/sun.vert", "shaders/sun.frag")
        loadShader("ring", "shaders/ring.vert", "shaders/ring.frag")
        loadShader("skybox", "shaders/skybox.vert", "shaders/skybox.frag")
        loadShader("label", "shaders/label.vert", "shaders/label.frag")
    }

    fun releaseAll() {
        programs.values.forEach { it.release() }
        programs.clear()
    }

    private fun loadAsset(filename: String): String? {
        return try {
            context.assets.open(filename).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load asset: $filename", e)
            null
        }
    }

    companion object {
        private const val TAG = "ShaderManager"
    }
}

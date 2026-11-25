package com.cosmicwall.solarsystem.gl

import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay

/**
 * Robust EGLConfigChooser with clean fallbacks for Android GLSurfaceView across Samsung Adreno GPUs.
 */
class CosmicEGLConfigChooser : GLSurfaceView.EGLConfigChooser {

    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
        val configsCount = IntArray(1)
        val spec = intArrayOf(
            EGL10.EGL_RENDERABLE_TYPE, 4, // EGL_OPENGL_ES2_BIT (compatible with ES 3)
            EGL10.EGL_SURFACE_TYPE, EGL10.EGL_WINDOW_BIT,
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_DEPTH_SIZE, 16,
            EGL10.EGL_NONE
        )
        egl.eglChooseConfig(display, spec, null, 0, configsCount)
        val numConfigs = configsCount[0]
        if (numConfigs <= 0) {
            throw IllegalArgumentException("No EGL configs match spec")
        }

        val configs = arrayOfNulls<EGLConfig>(numConfigs)
        egl.eglChooseConfig(display, spec, configs, numConfigs, configsCount)

        var bestConfig: EGLConfig? = null
        var bestScore = -1

        for (config in configs) {
            if (config == null) continue
            val r = findAttrib(egl, display, config, EGL10.EGL_RED_SIZE)
            val g = findAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE)
            val b = findAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE)
            val a = findAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE)
            val d = findAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE)

            if (r >= 8 && g >= 8 && b >= 8 && d >= 16) {
                var score = 0
                if (d >= 24) score += 100
                if (a >= 8) score += 10
                if (score > bestScore) {
                    bestScore = score
                    bestConfig = config
                }
            }
        }

        return bestConfig ?: configs[0]
            ?: throw IllegalArgumentException("Failed to choose valid EGLConfig")
    }

    private fun findAttrib(egl: EGL10, display: EGLDisplay, config: EGLConfig, attribute: Int): Int {
        val value = IntArray(1)
        return if (egl.eglGetConfigAttrib(display, config, attribute, value)) value[0] else 0
    }
}

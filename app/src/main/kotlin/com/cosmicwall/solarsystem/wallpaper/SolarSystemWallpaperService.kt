package com.cosmicwall.solarsystem.wallpaper

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import com.cosmicwall.solarsystem.interaction.CameraController
import com.cosmicwall.solarsystem.interaction.RayCaster
import com.cosmicwall.solarsystem.renderer.SolarSystemRenderer

class SolarSystemWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return GLEngine()
    }

    inner class GLEngine : Engine() {
        private var renderer: SolarSystemRenderer? = null
        private var renderThread: WallpaperGLThread? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            // Keep home screen touches completely unobstructed; react only to page swipes and unlock
            setTouchEventsEnabled(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setOffsetNotificationsEnabled(true)
            }

            val camera = CameraController()
            val rayCaster = RayCaster()
            val ren = SolarSystemRenderer(applicationContext, camera, rayCaster)
            renderer = ren
            renderThread = WallpaperGLThread(ren).also { it.start() }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            renderThread?.onSurfaceCreated(holder)
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            renderThread?.onSurfaceChanged(holder, width, height)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            renderThread?.onSurfaceDestroyed()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            renderThread?.setVisible(visible)
            if (visible) {
                renderer?.onWallpaperUnlocked()
            }
        }

        override fun onOffsetsChanged(
            xOffset: Float, yOffset: Float,
            xOffsetStep: Float, yOffsetStep: Float,
            xPixelOffset: Int, yPixelOffset: Int
        ) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset)
            renderer?.camera?.onOffsetChanged(xOffset)
        }

        override fun onDestroy() {
            super.onDestroy()
            renderThread?.requestExitAndWait()
            renderThread = null
            renderer = null
        }
    }

    private class WallpaperGLThread(
        private val renderer: SolarSystemRenderer
    ) : Thread("SolarWallpaperGLThread") {

        private val lock = Object()
        private var shouldExit = false
        private var exited = false

        private var hasSurface = false
        private var isVisible = false
        private var surfaceHolder: SurfaceHolder? = null
        private var width = 0
        private var height = 0

        private var sizeChanged = false
        private var forceRenderOneFrame = false
        private var needDestroySurface = false

        private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
        private var eglConfig: EGLConfig? = null
        private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
        private var glInitialized = false

        fun onSurfaceCreated(holder: SurfaceHolder) {
            synchronized(lock) {
                surfaceHolder = holder
                hasSurface = true
                needDestroySurface = false
                forceRenderOneFrame = true
                lock.notifyAll()
            }
        }

        fun onSurfaceChanged(holder: SurfaceHolder, w: Int, h: Int) {
            synchronized(lock) {
                surfaceHolder = holder
                width = w
                height = h
                hasSurface = true
                sizeChanged = true
                forceRenderOneFrame = true
                lock.notifyAll()
            }
        }

        fun onSurfaceDestroyed() {
            synchronized(lock) {
                hasSurface = false
                needDestroySurface = true
                lock.notifyAll()
                val deadline = System.currentTimeMillis() + 500L
                while (eglSurface != EGL14.EGL_NO_SURFACE && !exited && System.currentTimeMillis() < deadline) {
                    try {
                        lock.wait(100L)
                    } catch (_: InterruptedException) {
                        currentThread().interrupt()
                        break
                    }
                }
            }
        }

        fun setVisible(visible: Boolean) {
            synchronized(lock) {
                isVisible = visible
                if (visible) {
                    forceRenderOneFrame = true
                }
                lock.notifyAll()
            }
        }

        fun requestExitAndWait() {
            synchronized(lock) {
                shouldExit = true
                lock.notifyAll()
                val deadline = System.currentTimeMillis() + 1000L
                while (!exited && System.currentTimeMillis() < deadline) {
                    try {
                        lock.wait(100L)
                    } catch (_: InterruptedException) {
                        currentThread().interrupt()
                        break
                    }
                }
            }
        }

        override fun run() {
            try {
                renderLoop()
            } catch (t: Throwable) {
                Log.e(TAG, "GL wallpaper thread crashed", t)
            } finally {
                releaseEgl()
                synchronized(lock) {
                    exited = true
                    lock.notifyAll()
                }
            }
        }

        private fun renderLoop() {
            while (true) {
                var holderSnapshot: SurfaceHolder? = null
                var wSnapshot = 0
                var hSnapshot = 0
                var doSizeChanged = false
                var createSurfaceNow = false

                synchronized(lock) {
                    while (true) {
                        if (needDestroySurface || (!hasSurface && eglSurface != EGL14.EGL_NO_SURFACE)) {
                            destroyEglSurfaceLocked()
                            needDestroySurface = false
                            lock.notifyAll()
                        }

                        if (shouldExit) {
                            return
                        }

                        val ready = hasSurface && width > 0 && height > 0 && (isVisible || forceRenderOneFrame)
                        if (ready) {
                            holderSnapshot = surfaceHolder
                            wSnapshot = width
                            hSnapshot = height
                            doSizeChanged = sizeChanged
                            createSurfaceNow = (eglSurface == EGL14.EGL_NO_SURFACE)
                            sizeChanged = false
                            forceRenderOneFrame = false
                            break
                        }

                        try {
                            lock.wait()
                        } catch (_: InterruptedException) {
                            currentThread().interrupt()
                            return
                        }
                    }
                }

                if (!ensureEglContext()) {
                    sleepQuietly(50L)
                    continue
                }

                if (createSurfaceNow || eglSurface == EGL14.EGL_NO_SURFACE) {
                    val holder = holderSnapshot ?: continue
                    if (!createEglSurface(holder)) {
                        sleepQuietly(30L)
                        continue
                    }
                    doSizeChanged = true
                }

                if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                    Log.w(TAG, "eglMakeCurrent failed: ${EGL14.eglGetError()}")
                    synchronized(lock) { destroyEglSurfaceLocked() }
                    continue
                }

                if (!glInitialized) {
                    renderer.onSurfaceCreated(null, null)
                    glInitialized = true
                    doSizeChanged = true
                }

                if (doSizeChanged) {
                    renderer.onSurfaceChanged(null, wSnapshot, hSnapshot)
                }

                renderer.onDrawFrame(null)

                if (!EGL14.eglSwapBuffers(eglDisplay, eglSurface)) {
                    val err = EGL14.eglGetError()
                    Log.w(TAG, "eglSwapBuffers error: $err")
                    if (err == EGL14.EGL_CONTEXT_LOST) {
                        releaseEgl()
                    } else {
                        synchronized(lock) { destroyEglSurfaceLocked() }
                    }
                }
            }
        }

        private fun ensureEglContext(): Boolean {
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
                eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                if (eglDisplay == EGL14.EGL_NO_DISPLAY) return false
                val version = IntArray(2)
                if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
                    eglDisplay = EGL14.EGL_NO_DISPLAY
                    return false
                }
            }

            if (eglConfig == null) {
                eglConfig = chooseBestEglConfig(eglDisplay) ?: return false
            }

            if (eglContext == EGL14.EGL_NO_CONTEXT) {
                val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
                eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
                if (eglContext == EGL14.EGL_NO_CONTEXT) {
                    return false
                }
                glInitialized = false
            }
            return true
        }

        private fun createEglSurface(holder: SurfaceHolder): Boolean {
            val surface = holder.surface
            if (surface == null || !surface.isValid) return false
            val attribs = intArrayOf(EGL14.EGL_NONE)
            eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, attribs, 0)
            if (eglSurface == EGL14.EGL_NO_SURFACE) {
                Log.w(TAG, "eglCreateWindowSurface failed: ${EGL14.eglGetError()}")
                return false
            }
            return true
        }

        private fun destroyEglSurfaceLocked() {
            if (eglDisplay != EGL14.EGL_NO_DISPLAY && eglSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                EGL14.eglDestroySurface(eglDisplay, eglSurface)
                eglSurface = EGL14.EGL_NO_SURFACE
            }
        }

        private fun releaseEgl() {
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                if (glInitialized && eglContext != EGL14.EGL_NO_CONTEXT) {
                    if (eglSurface != EGL14.EGL_NO_SURFACE) {
                        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
                    } else {
                        EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, eglContext)
                    }
                    try {
                        renderer.release()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error releasing renderer", e)
                    }
                    glInitialized = false
                }
                synchronized(lock) {
                    destroyEglSurfaceLocked()
                }
                if (eglContext != EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglDestroyContext(eglDisplay, eglContext)
                    eglContext = EGL14.EGL_NO_CONTEXT
                }
                EGL14.eglTerminate(eglDisplay)
                eglDisplay = EGL14.EGL_NO_DISPLAY
            }
            eglConfig = null
        }

        private fun chooseBestEglConfig(display: EGLDisplay): EGLConfig? {
            val attempts = listOf(
                intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_DEPTH_SIZE, 24,
                    EGL14.EGL_NONE
                ),
                intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_DEPTH_SIZE, 24,
                    EGL14.EGL_NONE
                ),
                intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_DEPTH_SIZE, 16,
                    EGL14.EGL_NONE
                )
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            for (spec in attempts) {
                if (EGL14.eglChooseConfig(display, spec, 0, configs, 0, 1, numConfigs, 0) && numConfigs[0] > 0) {
                    return configs[0]
                }
            }
            return null
        }

        private fun sleepQuietly(ms: Long) {
            try {
                sleep(ms)
            } catch (_: InterruptedException) {
                currentThread().interrupt()
            }
        }
    }

    companion object {
        private const val TAG = "SolarWallpaperService"
    }
}

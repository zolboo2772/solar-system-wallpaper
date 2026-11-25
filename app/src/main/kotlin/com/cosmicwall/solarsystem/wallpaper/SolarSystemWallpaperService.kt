package com.cosmicwall.solarsystem.wallpaper

import android.content.Context
import android.opengl.GLSurfaceView
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.cosmicwall.solarsystem.interaction.CameraController
import com.cosmicwall.solarsystem.interaction.RayCaster
import com.cosmicwall.solarsystem.renderer.SolarSystemRenderer

class SolarSystemWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return GLEngine()
    }

    inner class GLEngine : Engine() {
        private var glSurfaceView: WallpaperGLSurfaceView? = null
        private var renderer: SolarSystemRenderer? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            // Do not intercept home screen touch/drag events; only react to page swipes (onOffsetsChanged) and unlock
            setTouchEventsEnabled(false)

            val camera = CameraController()
            val rayCaster = RayCaster()
            val ren = SolarSystemRenderer(applicationContext, camera, rayCaster)
            renderer = ren

            glSurfaceView = WallpaperGLSurfaceView(applicationContext).apply {
                setEGLContextClientVersion(3)
                setEGLConfigChooser(8, 8, 8, 8, 24, 0)
                setPreserveEGLContextOnPause(true)
                setRenderer(ren)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            glSurfaceView?.surfaceCreated(holder)
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            glSurfaceView?.surfaceChanged(holder, format, width, height)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            glSurfaceView?.surfaceDestroyed(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                glSurfaceView?.onResume()
                glSurfaceView?.queueEvent {
                    renderer?.onWallpaperUnlocked()
                }
            } else {
                glSurfaceView?.onPause()
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
            glSurfaceView?.queueEvent {
                renderer?.release()
            }
            glSurfaceView?.onPause()
            glSurfaceView?.onWallpaperDestroy()
            glSurfaceView = null
            renderer = null
        }

        inner class WallpaperGLSurfaceView(context: Context) : GLSurfaceView(context) {
            private var hasSurface = false

            override fun getHolder(): SurfaceHolder {
                return surfaceHolder
            }

            override fun surfaceCreated(holder: SurfaceHolder) {
                if (!hasSurface) {
                    hasSurface = true
                    super.surfaceCreated(holder)
                }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                if (hasSurface) {
                    hasSurface = false
                    super.surfaceDestroyed(holder)
                }
            }

            fun onWallpaperDestroy() {
                super.onDetachedFromWindow()
            }
        }
    }
}

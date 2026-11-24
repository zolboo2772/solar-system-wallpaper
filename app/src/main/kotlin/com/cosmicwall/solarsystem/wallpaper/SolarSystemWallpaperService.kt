package com.cosmicwall.solarsystem.wallpaper

import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.cosmicwall.solarsystem.interaction.CameraController
import com.cosmicwall.solarsystem.interaction.RayCaster
import com.cosmicwall.solarsystem.interaction.TouchHandler
import com.cosmicwall.solarsystem.renderer.SolarSystemRenderer

class SolarSystemWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return GLEngine()
    }

    inner class GLEngine : Engine() {
        private var glSurfaceView: WallpaperGLSurfaceView? = null
        private var renderer: SolarSystemRenderer? = null
        private var touchHandler: TouchHandler? = null
        private var rendererSet = false

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)

            val camera = CameraController()
            val rayCaster = RayCaster()
            val ren = SolarSystemRenderer(applicationContext, camera, rayCaster)
            renderer = ren

            touchHandler = TouchHandler(applicationContext, camera, rayCaster).apply {
                planets = ren.planets
                onPlanetTapped = { planet ->
                    renderer?.showPlanetLabel(planet)
                }
            }

            glSurfaceView = WallpaperGLSurfaceView(applicationContext).apply {
                setSurfaceHolder(surfaceHolder)
                setEGLContextClientVersion(3)
                setEGLConfigChooser(8, 8, 8, 8, 24, 0)
                setRenderer(ren)
                renderMode = android.opengl.GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }

            rendererSet = true
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (!rendererSet) return

            if (visible) {
                glSurfaceView?.onResume()
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

        override fun onTouchEvent(event: MotionEvent?) {
            super.onTouchEvent(event)
            event?.let { touchHandler?.onTouchEvent(it) }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            if (rendererSet) {
                glSurfaceView?.onPause()
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            if (rendererSet) {
                glSurfaceView?.onPause()
                glSurfaceView?.onWallpaperDestroy()
                renderer?.release()
            }
            glSurfaceView = null
            renderer = null
            touchHandler = null
            rendererSet = false
        }
    }
}

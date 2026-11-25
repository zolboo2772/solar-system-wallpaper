package com.cosmicwall.solarsystem.interaction

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.cosmicwall.solarsystem.scene.CelestialBody

class TouchHandler(
    context: Context,
    private val camera: CameraController,
    private val rayCaster: RayCaster
) {
    var planets: List<CelestialBody> = emptyList()
    var onPlanetTapped: ((CelestialBody) -> Unit)? = null

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            camera.onDrag(-distanceX, -distanceY)
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val tappedPlanet = rayCaster.pickPlanet(
                e.x, e.y, camera, planets
            )
            if (tappedPlanet != null) {
                val focusDist = if (tappedPlanet.config.name == "Saturn") 2.8f else tappedPlanet.config.radius * 4.3f
                camera.focusOnTarget(tappedPlanet.worldPosition, focusDist)
                onPlanetTapped?.invoke(tappedPlanet)
            }
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            camera.resetView()
            return true
        }
    })

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            camera.onScale(detector.scaleFactor)
            return true
        }
    })

    fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = scaleDetector.onTouchEvent(event)
        handled = gestureDetector.onTouchEvent(event) || handled
        return handled
    }
}

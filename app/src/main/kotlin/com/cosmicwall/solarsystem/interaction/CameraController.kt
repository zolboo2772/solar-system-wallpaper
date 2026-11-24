package com.cosmicwall.solarsystem.interaction

import android.opengl.Matrix
import com.cosmicwall.solarsystem.util.Vector3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.min

class CameraController {
    var azimuth = 45f
    var elevation = 25f
    var distance = 18f

    private var targetAzimuth = azimuth
    private var targetElevation = elevation
    private var targetDistance = distance

    var lookAtTarget = Vector3.ZERO
    private var currentLookAt = Vector3.ZERO

    var autoRotateSpeed = 3f
    var isAutoRotating = true
    private var idleTime = 0f
    private val idleThreshold = 3f

    private var azimuthVelocity = 0f
    private var elevationVelocity = 0f
    private val damping = 0.92f
    private val smoothSpeed = 5f

    var position = Vector3.ZERO
        private set

    val viewMatrix = FloatArray(16)
    val projectionMatrix = FloatArray(16)

    var parallaxOffset = 0f

    fun onDrag(deltaX: Float, deltaY: Float) {
        azimuthVelocity = deltaX * 0.3f
        elevationVelocity = -deltaY * 0.3f
        targetAzimuth += azimuthVelocity
        targetElevation = (targetElevation + elevationVelocity).coerceIn(5f, 85f)
        idleTime = 0f
    }

    fun onScale(scaleFactor: Float) {
        targetDistance = (targetDistance / scaleFactor).coerceIn(4f, 30f)
        idleTime = 0f
    }

    fun focusOnTarget(target: Vector3) {
        lookAtTarget = target
        targetDistance = 3f
        idleTime = 0f
    }

    fun resetView() {
        lookAtTarget = Vector3.ZERO
        targetAzimuth = 45f
        targetElevation = 25f
        targetDistance = 18f
        idleTime = 0f
    }

    fun onOffsetChanged(xOffset: Float) {
        parallaxOffset = (xOffset - 0.5f) * 15f
    }

    fun update(deltaTime: Float) {
        azimuthVelocity *= damping
        elevationVelocity *= damping

        targetAzimuth += azimuthVelocity * deltaTime
        targetElevation = (targetElevation + elevationVelocity * deltaTime).coerceIn(5f, 85f)

        idleTime += deltaTime
        if (isAutoRotating && idleTime > idleThreshold) {
            targetAzimuth += autoRotateSpeed * deltaTime
        }

        val t = min(1f, smoothSpeed * deltaTime)
        azimuth += (targetAzimuth - azimuth) * t
        elevation += (targetElevation - elevation) * t
        distance += (targetDistance - distance) * t
        currentLookAt = currentLookAt.lerp(lookAtTarget, t)

        val azRad = Math.toRadians((azimuth + parallaxOffset).toDouble())
        val elRad = Math.toRadians(elevation.toDouble())

        position = Vector3(
            (distance * cos(elRad) * sin(azRad)).toFloat() + currentLookAt.x,
            (distance * sin(elRad)).toFloat() + currentLookAt.y,
            (distance * cos(elRad) * cos(azRad)).toFloat() + currentLookAt.z
        )

        Matrix.setLookAtM(
            viewMatrix, 0,
            position.x, position.y, position.z,
            currentLookAt.x, currentLookAt.y, currentLookAt.z,
            0f, 1f, 0f
        )
    }

    fun updateProjection(width: Int, height: Int) {
        val aspect = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 45f, aspect, 0.1f, 100f)
    }
}

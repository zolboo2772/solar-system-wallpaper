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

    private var heroDistance = 18f
    private var heroElevation = 25f
    private var minDistance = 0.25f
    private var maxDistance = 35f
    var isHeroMode = false
        private set

    var parallaxOffset = 0f

    fun onDrag(deltaX: Float, deltaY: Float) {
        azimuthVelocity = deltaX * 0.3f
        elevationVelocity = -deltaY * 0.3f
        targetAzimuth += azimuthVelocity
        targetElevation = (targetElevation + elevationVelocity).coerceIn(-65f, 85f)
        idleTime = 0f
    }

    fun onScale(scaleFactor: Float) {
        targetDistance = (targetDistance / scaleFactor).coerceIn(minDistance, maxDistance)
        idleTime = 0f
    }

    fun setHeroMode(distance: Float, elevation: Float = 14f, azimuthHint: Float? = null) {
        isHeroMode = true
        heroDistance = distance
        heroElevation = elevation
        minDistance = (distance * 0.45f).coerceAtLeast(0.22f)
        maxDistance = (distance * 4.5f).coerceAtLeast(18f)
        targetDistance = distance
        targetElevation = elevation
        if (azimuthHint != null) {
            targetAzimuth = azimuthHint
        }
        idleTime = 0f
    }

    fun setSystemMode() {
        isHeroMode = false
        heroDistance = 18f
        heroElevation = 25f
        minDistance = 0.5f
        maxDistance = 35f
        lookAtTarget = Vector3.ZERO
        targetDistance = 18f
        targetElevation = 25f
        idleTime = 0f
    }

    fun triggerXiaomiUnlockZoom() {
        val zoomMult = if (isHeroMode) 2.7f else 1.6f
        distance = (targetDistance * zoomMult).coerceAtMost(maxDistance)
        elevation = (targetElevation + 18f).coerceIn(-65f, 85f)
        azimuth = targetAzimuth - 32f
        idleTime = 0f
    }

    fun trackTarget(target: Vector3) {
        lookAtTarget = target
    }

    fun focusOnTarget(target: Vector3, focusDistance: Float = 1.2f) {
        lookAtTarget = target
        targetDistance = focusDistance.coerceIn(minDistance, maxDistance)
        idleTime = 0f
    }

    fun resetView() {
        if (!isHeroMode) {
            lookAtTarget = Vector3.ZERO
            targetAzimuth = 45f
        }
        targetElevation = heroElevation
        targetDistance = heroDistance
        idleTime = 0f
    }

    fun onOffsetChanged(xOffset: Float) {
        val sweep = if (isHeroMode) 50f else 20f
        parallaxOffset = (xOffset - 0.5f) * sweep
    }

    fun update(deltaTime: Float) {
        azimuthVelocity *= damping
        elevationVelocity *= damping

        targetAzimuth += azimuthVelocity * deltaTime
        targetElevation = (targetElevation + elevationVelocity * deltaTime).coerceIn(-65f, 85f)

        idleTime += deltaTime
        if (isAutoRotating && idleTime > idleThreshold) {
            targetAzimuth += autoRotateSpeed * deltaTime
        }

        val t = min(1f, smoothSpeed * deltaTime)
        val lookAtT = if (isHeroMode) min(1f, 14f * deltaTime) else t
        azimuth += (targetAzimuth - azimuth) * t
        elevation += (targetElevation - elevation) * t
        distance += (targetDistance - distance) * t
        currentLookAt = currentLookAt.lerp(lookAtTarget, lookAtT)

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
        val w = if (width <= 0) 1 else width
        val h = if (height <= 0) 1 else height
        val aspect = w.toFloat() / h.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 45f, aspect, 0.03f, 150f)
    }
}

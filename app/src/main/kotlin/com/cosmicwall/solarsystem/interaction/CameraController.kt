package com.cosmicwall.solarsystem.interaction

import android.opengl.Matrix
import com.cosmicwall.solarsystem.util.Vector3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.min
import kotlin.math.pow

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
    private var elapsedTime = 0f

    private var heroAnchorAzimuth = 45f
    private var userAzimuthOffset = 0f

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
        if (isHeroMode) {
            userAzimuthOffset += azimuthVelocity
        } else {
            targetAzimuth += azimuthVelocity
        }
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
        userAzimuthOffset = 0f
        if (azimuthHint != null) {
            heroAnchorAzimuth = azimuthHint
            targetAzimuth = azimuthHint
        }
        idleTime = 0f
    }

    fun updateHeroSunAngle(azimuthHint: Float) {
        if (!isHeroMode) return
        heroAnchorAzimuth = azimuthHint
    }

    fun setSystemMode() {
        isHeroMode = false
        heroDistance = 18f
        heroElevation = 25f
        minDistance = 0.5f
        maxDistance = 35f
        userAzimuthOffset = 0f
        lookAtTarget = Vector3.ZERO
        targetDistance = 18f
        targetElevation = 25f
        idleTime = 0f
    }

    fun triggerXiaomiUnlockZoom() {
        val zoomMult = if (isHeroMode) 1.95f else 1.45f
        distance = (targetDistance * zoomMult).coerceAtMost(maxDistance)
        elevation = (targetElevation + 12f).coerceIn(-65f, 85f)
        azimuth = targetAzimuth - 16f
        idleTime = 0f
    }

    fun snapToTarget(target: Vector3) {
        lookAtTarget = target
        currentLookAt = target
    }

    fun trackTarget(target: Vector3) {
        lookAtTarget = target
        if (isHeroMode) {
            currentLookAt = target
        }
    }

    fun focusOnTarget(target: Vector3, focusDistance: Float = 1.2f) {
        lookAtTarget = target
        targetDistance = focusDistance.coerceIn(minDistance, maxDistance)
        idleTime = 0f
    }

    fun resetView() {
        userAzimuthOffset = 0f
        if (!isHeroMode) {
            lookAtTarget = Vector3.ZERO
            targetAzimuth = 45f
        } else {
            targetAzimuth = heroAnchorAzimuth
        }
        targetElevation = heroElevation
        targetDistance = heroDistance
        idleTime = 0f
    }

    fun onOffsetChanged(xOffset: Float) {
        val sweep = if (isHeroMode) 45f else 20f
        parallaxOffset = (xOffset - 0.5f) * sweep
    }

    fun update(deltaTime: Float) {
        elapsedTime += deltaTime
        val dampFactor = damping.pow((deltaTime * 60f).coerceIn(0.1f, 3f))
        azimuthVelocity *= dampFactor
        elevationVelocity *= dampFactor

        if (isHeroMode) {
            userAzimuthOffset += azimuthVelocity * deltaTime * 2.5f
            val breath = if (isAutoRotating) sin((elapsedTime * 0.22).toDouble()).toFloat() * 8f else 0f
            targetAzimuth = heroAnchorAzimuth + userAzimuthOffset + breath
        } else {
            targetAzimuth += azimuthVelocity * deltaTime
            idleTime += deltaTime
            if (isAutoRotating && idleTime > idleThreshold) {
                targetAzimuth += autoRotateSpeed * deltaTime
            }
        }
        targetElevation = (targetElevation + elevationVelocity * deltaTime).coerceIn(-65f, 85f)

        val t = min(1f, smoothSpeed * deltaTime)
        azimuth += (targetAzimuth - azimuth) * t
        elevation += (targetElevation - elevation) * t
        distance += (targetDistance - distance) * t
        currentLookAt = if (isHeroMode) lookAtTarget else currentLookAt.lerp(lookAtTarget, t)

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
        // Expand vertical FOV automatically in portrait so planets and Saturn's rings frame completely
        val fovY = if (aspect < 1f) {
            (45f / kotlin.math.sqrt(aspect)).coerceIn(45f, 66f)
        } else {
            45f
        }
        Matrix.perspectiveM(projectionMatrix, 0, fovY, aspect, 0.03f, 150f)
    }
}

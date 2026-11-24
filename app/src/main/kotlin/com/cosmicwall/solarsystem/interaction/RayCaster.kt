package com.cosmicwall.solarsystem.interaction

import android.opengl.Matrix
import com.cosmicwall.solarsystem.scene.CelestialBody
import com.cosmicwall.solarsystem.util.Vector3
import kotlin.math.sqrt

class RayCaster {
    private var viewportWidth = 0
    private var viewportHeight = 0

    fun setViewport(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
    }

    fun pickPlanet(
        screenX: Float,
        screenY: Float,
        camera: CameraController,
        planets: List<CelestialBody>
    ): CelestialBody? {
        if (viewportWidth == 0 || viewportHeight == 0) return null

        val rayOrigin = camera.position
        val rayDir = unproject(screenX, screenY, camera.viewMatrix, camera.projectionMatrix)
            ?: return null

        var closestPlanet: CelestialBody? = null
        var closestDist = Float.MAX_VALUE

        for (planet in planets) {
            val dist = raySphereIntersect(
                rayOrigin, rayDir,
                planet.worldPosition,
                planet.config.radius * 1.8f
            )
            if (dist != null && dist < closestDist) {
                closestDist = dist
                closestPlanet = planet
            }
        }

        return closestPlanet
    }

    private fun unproject(
        screenX: Float,
        screenY: Float,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray
    ): Vector3? {
        val glY = viewportHeight - screenY

        val vpMatrix = FloatArray(16)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        val invVP = FloatArray(16)
        if (!Matrix.invertM(invVP, 0, vpMatrix, 0)) return null

        val ndcX = (2f * screenX / viewportWidth) - 1f
        val ndcY = (2f * glY / viewportHeight) - 1f

        val nearPoint = floatArrayOf(ndcX, ndcY, -1f, 1f)
        val worldNear = FloatArray(4)
        Matrix.multiplyMV(worldNear, 0, invVP, 0, nearPoint, 0)
        if (worldNear[3] == 0f) return null
        val near = Vector3(worldNear[0] / worldNear[3], worldNear[1] / worldNear[3], worldNear[2] / worldNear[3])

        val farPoint = floatArrayOf(ndcX, ndcY, 1f, 1f)
        val worldFar = FloatArray(4)
        Matrix.multiplyMV(worldFar, 0, invVP, 0, farPoint, 0)
        if (worldFar[3] == 0f) return null
        val far = Vector3(worldFar[0] / worldFar[3], worldFar[1] / worldFar[3], worldFar[2] / worldFar[3])

        return (far - near).normalized()
    }

    private fun raySphereIntersect(
        rayOrigin: Vector3,
        rayDir: Vector3,
        sphereCenter: Vector3,
        sphereRadius: Float
    ): Float? {
        val oc = rayOrigin - sphereCenter
        val a = rayDir.dot(rayDir)
        val b = 2f * oc.dot(rayDir)
        val c = oc.dot(oc) - sphereRadius * sphereRadius
        val discriminant = b * b - 4f * a * c

        if (discriminant < 0f) return null

        val t = (-b - sqrt(discriminant)) / (2f * a)
        return if (t > 0f) t else null
    }
}

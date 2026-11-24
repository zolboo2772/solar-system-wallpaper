package com.cosmicwall.solarsystem.util

import kotlin.math.sqrt

data class Vector3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    companion object {
        val ZERO get() = Vector3(0f, 0f, 0f)
        val UP get() = Vector3(0f, 1f, 0f)
        val FORWARD get() = Vector3(0f, 0f, -1f)
        val RIGHT get() = Vector3(1f, 0f, 0f)
    }

    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = Vector3(x / scalar, y / scalar, z / scalar)

    fun length(): Float = sqrt(x * x + y * y + z * z)
    fun lengthSquared(): Float = x * x + y * y + z * z

    fun normalized(): Vector3 {
        val len = length()
        return if (len > 0.0001f) Vector3(x / len, y / len, z / len) else ZERO
    }

    fun dot(other: Vector3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3) = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun lerp(target: Vector3, t: Float): Vector3 {
        val clamped = t.coerceIn(0f, 1f)
        return Vector3(
            x + (target.x - x) * clamped,
            y + (target.y - y) * clamped,
            z + (target.z - z) * clamped
        )
    }

    fun distanceTo(other: Vector3): Float = (this - other).length()

    fun toFloatArray() = floatArrayOf(x, y, z)

    override fun toString() = "Vector3($x, $y, $z)"
}

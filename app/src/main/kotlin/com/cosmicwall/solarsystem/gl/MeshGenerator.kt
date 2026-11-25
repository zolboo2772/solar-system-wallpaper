package com.cosmicwall.solarsystem.gl

import android.opengl.GLES32
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

class Mesh(
    val vboId: Int,
    val iboId: Int,
    val indexCount: Int,
    val stride: Int
) {
    fun release() {
        if (vboId != 0) GLES32.glDeleteBuffers(1, intArrayOf(vboId), 0)
        if (iboId != 0) GLES32.glDeleteBuffers(1, intArrayOf(iboId), 0)
    }
}

object MeshGenerator {

    fun generateSphere(latSegments: Int = 64, lonSegments: Int = 128): Mesh {
        val vertices = mutableListOf<Float>()
        val indices = mutableListOf<Int>()

        for (lat in 0..latSegments) {
            val theta = lat * PI.toFloat() / latSegments
            val sinTheta = sin(theta)
            val cosTheta = cos(theta)

            for (lon in 0..lonSegments) {
                val phi = lon * 2f * PI.toFloat() / lonSegments
                val sinPhi = sin(phi)
                val cosPhi = cos(phi)

                val x = cosPhi * sinTheta
                val y = cosTheta
                val z = sinPhi * sinTheta
                vertices.addAll(listOf(x, y, z))

                vertices.addAll(listOf(x, y, z))

                val u = lon.toFloat() / lonSegments
                val v = lat.toFloat() / latSegments
                vertices.addAll(listOf(u, v))

                val tx = -sinPhi
                val ty = 0f
                val tz = cosPhi
                val tw = 1f
                vertices.addAll(listOf(tx, ty, tz, tw))
            }
        }

        for (lat in 0 until latSegments) {
            for (lon in 0 until lonSegments) {
                val first = lat * (lonSegments + 1) + lon
                val second = first + lonSegments + 1

                // Front-facing counter-clockwise winding order (CCW)
                indices.add(first)
                indices.add(first + 1)
                indices.add(second)

                indices.add(second)
                indices.add(first + 1)
                indices.add(second + 1)
            }
        }

        return uploadMesh(vertices.toFloatArray(), indices.toIntArray(), 12 * 4)
    }

    fun generateRingDisk(innerRadius: Float, outerRadius: Float, segments: Int = 160): Mesh {
        val vertices = mutableListOf<Float>()
        val indices = mutableListOf<Int>()

        for (i in 0..segments) {
            val angle = i * 2f * PI.toFloat() / segments
            val cosA = cos(angle)
            val sinA = sin(angle)

            vertices.addAll(listOf(innerRadius * cosA, 0f, innerRadius * sinA))
            vertices.addAll(listOf(0f, 0.5f))

            vertices.addAll(listOf(outerRadius * cosA, 0f, outerRadius * sinA))
            vertices.addAll(listOf(1f, 0.5f))
        }

        for (i in 0 until segments) {
            val idx = i * 2
            indices.add(idx)
            indices.add(idx + 1)
            indices.add(idx + 2)

            indices.add(idx + 1)
            indices.add(idx + 3)
            indices.add(idx + 2)
        }

        return uploadMesh(vertices.toFloatArray(), indices.toIntArray(), 5 * 4)
    }

    private fun uploadMesh(vertices: FloatArray, indices: IntArray, stride: Int): Mesh {
        val vboIds = IntArray(1)
        val iboIds = IntArray(1)

        GLES32.glGenBuffers(1, vboIds, 0)
        GLES32.glGenBuffers(1, iboIds, 0)

        val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        vertexBuffer.position(0)

        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vboIds[0])
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, vertices.size * 4, vertexBuffer, GLES32.GL_STATIC_DRAW)

        val indexBuffer: IntBuffer = ByteBuffer.allocateDirect(indices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
            .put(indices)
        indexBuffer.position(0)

        GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, iboIds[0])
        GLES32.glBufferData(GLES32.GL_ELEMENT_ARRAY_BUFFER, indices.size * 4, indexBuffer, GLES32.GL_STATIC_DRAW)

        return Mesh(vboIds[0], iboIds[0], indices.size, stride)
    }
}

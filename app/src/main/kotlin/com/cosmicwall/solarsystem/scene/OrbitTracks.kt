package com.cosmicwall.solarsystem.scene

import android.opengl.GLES32
import android.opengl.Matrix
import com.cosmicwall.solarsystem.gl.ShaderProgram
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class OrbitTracks {
    private var vboId = 0
    private val segments = 180
    private val modelMatrix = FloatArray(16)

    fun init() {
        val vertices = FloatArray(segments * 3)
        for (i in 0 until segments) {
            val angle = (i.toFloat() / segments) * (2f * PI.toFloat())
            vertices[i * 3] = cos(angle)
            vertices[i * 3 + 1] = 0f
            vertices[i * 3 + 2] = sin(angle)
        }

        val buffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        buffer.position(0)

        val ids = IntArray(1)
        GLES32.glGenBuffers(1, ids, 0)
        vboId = ids[0]

        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vboId)
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, vertices.size * 4, buffer, GLES32.GL_STATIC_DRAW)
    }

    fun draw(
        shader: ShaderProgram,
        planets: List<CelestialBody>,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray
    ) {
        if (vboId == 0) return
        shader.use()
        shader.setUniformMatrix4fv("u_viewMatrix", viewMatrix)
        shader.setUniformMatrix4fv("u_projectionMatrix", projectionMatrix)

        GLES32.glEnable(GLES32.GL_BLEND)
        GLES32.glBlendFunc(GLES32.GL_SRC_ALPHA, GLES32.GL_ONE_MINUS_SRC_ALPHA)
        GLES32.glDepthMask(false)

        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vboId)
        GLES32.glEnableVertexAttribArray(0)
        GLES32.glVertexAttribPointer(0, 3, GLES32.GL_FLOAT, false, 3 * 4, 0)

        for (planet in planets) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.scaleM(modelMatrix, 0, planet.config.orbitRadius, 1f, planet.config.orbitRadius)
            shader.setUniformMatrix4fv("u_modelMatrix", modelMatrix)

            val alpha = if (planet.config.name == "Earth") 0.34f else 0.18f
            shader.setUniform4f("u_color", 0.58f, 0.72f, 1.0f, alpha)
            GLES32.glDrawArrays(GLES32.GL_LINE_LOOP, 0, segments)
        }

        GLES32.glDisableVertexAttribArray(0)
        GLES32.glDepthMask(true)
        GLES32.glDisable(GLES32.GL_BLEND)
    }

    fun release() {
        if (vboId != 0) {
            GLES32.glDeleteBuffers(1, intArrayOf(vboId), 0)
            vboId = 0
        }
    }
}

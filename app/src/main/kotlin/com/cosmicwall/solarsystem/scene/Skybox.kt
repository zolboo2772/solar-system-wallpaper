package com.cosmicwall.solarsystem.scene

import android.opengl.GLES32
import com.cosmicwall.solarsystem.gl.ShaderProgram
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Skybox {
    private var vboId = 0

    private val vertices = floatArrayOf(
        -1f,  1f, -1f,  -1f, -1f, -1f,   1f, -1f, -1f,
         1f, -1f, -1f,   1f,  1f, -1f,  -1f,  1f, -1f,
        -1f, -1f,  1f,  -1f, -1f, -1f,  -1f,  1f, -1f,
        -1f,  1f, -1f,  -1f,  1f,  1f,  -1f, -1f,  1f,
         1f, -1f, -1f,   1f, -1f,  1f,   1f,  1f,  1f,
         1f,  1f,  1f,   1f,  1f, -1f,   1f, -1f, -1f,
        -1f, -1f,  1f,  -1f,  1f,  1f,   1f,  1f,  1f,
         1f,  1f,  1f,   1f, -1f,  1f,  -1f, -1f,  1f,
        -1f,  1f, -1f,   1f,  1f, -1f,   1f,  1f,  1f,
         1f,  1f,  1f,  -1f,  1f,  1f,  -1f,  1f, -1f,
        -1f, -1f, -1f,  -1f, -1f,  1f,   1f, -1f, -1f,
         1f, -1f, -1f,  -1f, -1f,  1f,   1f, -1f,  1f
    )

    fun init() {
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

    fun draw(shader: ShaderProgram, viewMatrix: FloatArray, projectionMatrix: FloatArray,
             starDensity: Float, time: Float) {
        shader.use()

        GLES32.glDepthFunc(GLES32.GL_LEQUAL)
        GLES32.glDepthMask(false)

        shader.setUniformMatrix4fv("u_viewMatrix", viewMatrix)
        shader.setUniformMatrix4fv("u_projectionMatrix", projectionMatrix)
        shader.setUniform1f("u_starDensity", starDensity)
        shader.setUniform1f("u_time", time)

        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vboId)
        GLES32.glEnableVertexAttribArray(0)
        GLES32.glVertexAttribPointer(0, 3, GLES32.GL_FLOAT, false, 3 * 4, 0)

        GLES32.glDrawArrays(GLES32.GL_TRIANGLES, 0, 36)

        GLES32.glDisableVertexAttribArray(0)
        GLES32.glDepthMask(true)
        GLES32.glDepthFunc(GLES32.GL_LESS)
    }

    fun release() {
        if (vboId != 0) {
            GLES32.glDeleteBuffers(1, intArrayOf(vboId), 0)
            vboId = 0
        }
    }
}

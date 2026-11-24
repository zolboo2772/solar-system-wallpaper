package com.cosmicwall.solarsystem.gl

import android.opengl.GLES32
import android.util.Log

class ShaderProgram(
    private val vertexSource: String,
    private val fragmentSource: String
) {
    var programId: Int = 0
        private set

    fun compile(): Boolean {
        val vertexShader = compileShader(GLES32.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES32.GL_FRAGMENT_SHADER, fragmentSource)
        if (vertexShader == 0 || fragmentShader == 0) return false

        programId = GLES32.glCreateProgram()
        GLES32.glAttachShader(programId, vertexShader)
        GLES32.glAttachShader(programId, fragmentShader)
        GLES32.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES32.glGetProgramiv(programId, GLES32.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Program link failed: ${GLES32.glGetProgramInfoLog(programId)}")
            GLES32.glDeleteProgram(programId)
            programId = 0
            return false
        }

        GLES32.glDeleteShader(vertexShader)
        GLES32.glDeleteShader(fragmentShader)
        return true
    }

    fun use() { GLES32.glUseProgram(programId) }

    fun getUniformLocation(name: String): Int = GLES32.glGetUniformLocation(programId, name)
    fun getAttribLocation(name: String): Int = GLES32.glGetAttribLocation(programId, name)

    fun setUniformMatrix4fv(name: String, matrix: FloatArray) {
        GLES32.glUniformMatrix4fv(getUniformLocation(name), 1, false, matrix, 0)
    }

    fun setUniform3f(name: String, x: Float, y: Float, z: Float) {
        GLES32.glUniform3f(getUniformLocation(name), x, y, z)
    }

    fun setUniform1f(name: String, value: Float) {
        GLES32.glUniform1f(getUniformLocation(name), value)
    }

    fun setUniform1i(name: String, value: Int) {
        GLES32.glUniform1i(getUniformLocation(name), value)
    }

    fun release() {
        if (programId != 0) {
            GLES32.glDeleteProgram(programId)
            programId = 0
        }
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES32.glCreateShader(type)
        GLES32.glShaderSource(shader, source)
        GLES32.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES32.glGetShaderiv(shader, GLES32.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val typeName = if (type == GLES32.GL_VERTEX_SHADER) "vertex" else "fragment"
            Log.e(TAG, "$typeName shader compile failed: ${GLES32.glGetShaderInfoLog(shader)}")
            GLES32.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    companion object {
        private const val TAG = "ShaderProgram"
    }
}

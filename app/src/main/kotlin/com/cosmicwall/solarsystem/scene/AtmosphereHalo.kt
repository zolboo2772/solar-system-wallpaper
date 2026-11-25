package com.cosmicwall.solarsystem.scene

import android.opengl.GLES32
import android.opengl.Matrix
import com.cosmicwall.solarsystem.gl.Mesh
import com.cosmicwall.solarsystem.gl.ShaderProgram
import com.cosmicwall.solarsystem.util.Vector3

/**
 * Renders a volumetric outer atmospheric rim glow (or solar corona) around planets and the Sun
 * by drawing a slightly oversized back-face sphere shell with additive radial falloff.
 */
class AtmosphereHalo {
    private val modelMatrix = FloatArray(16)

    fun draw(
        shader: ShaderProgram,
        sphereMesh: Mesh,
        center: Vector3,
        shellRadius: Float,
        color: FloatArray,
        intensity: Float,
        power: Float,
        isSun: Boolean,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        cameraPosition: Vector3,
        lightDirection: FloatArray
    ) {
        shader.use()

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, center.x, center.y, center.z)
        Matrix.scaleM(modelMatrix, 0, shellRadius, shellRadius, shellRadius)

        shader.setUniformMatrix4fv("u_modelMatrix", modelMatrix)
        shader.setUniformMatrix4fv("u_viewMatrix", viewMatrix)
        shader.setUniformMatrix4fv("u_projectionMatrix", projectionMatrix)
        shader.setUniform3f("u_cameraPosition", cameraPosition.x, cameraPosition.y, cameraPosition.z)
        shader.setUniform3f("u_color", color[0], color[1], color[2])
        shader.setUniform3f("u_lightDirection", lightDirection[0], lightDirection[1], lightDirection[2])
        shader.setUniform1f("u_intensity", intensity)
        shader.setUniform1f("u_power", power)
        shader.setUniform1f("u_isSun", if (isSun) 1f else 0f)

        GLES32.glCullFace(GLES32.GL_FRONT)
        GLES32.glDepthMask(false)
        GLES32.glEnable(GLES32.GL_BLEND)
        GLES32.glBlendFunc(GLES32.GL_SRC_ALPHA, GLES32.GL_ONE)

        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, sphereMesh.vboId)
        GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, sphereMesh.iboId)

        val stride = sphereMesh.stride
        GLES32.glEnableVertexAttribArray(0)
        GLES32.glVertexAttribPointer(0, 3, GLES32.GL_FLOAT, false, stride, 0)
        GLES32.glEnableVertexAttribArray(1)
        GLES32.glVertexAttribPointer(1, 3, GLES32.GL_FLOAT, false, stride, 3 * 4)

        GLES32.glDrawElements(GLES32.GL_TRIANGLES, sphereMesh.indexCount, GLES32.GL_UNSIGNED_INT, 0)

        GLES32.glDisableVertexAttribArray(0)
        GLES32.glDisableVertexAttribArray(1)
        GLES32.glDisable(GLES32.GL_BLEND)
        GLES32.glDepthMask(true)
        GLES32.glCullFace(GLES32.GL_BACK)
    }
}

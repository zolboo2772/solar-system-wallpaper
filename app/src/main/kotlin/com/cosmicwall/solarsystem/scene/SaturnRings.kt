package com.cosmicwall.solarsystem.scene

import android.opengl.GLES32
import android.opengl.Matrix
import com.cosmicwall.solarsystem.gl.Mesh
import com.cosmicwall.solarsystem.gl.ShaderProgram
import com.cosmicwall.solarsystem.gl.TextureManager
import com.cosmicwall.solarsystem.util.Vector3

class SaturnRings {
    private val modelMatrix = FloatArray(16)

    fun draw(
        shader: ShaderProgram,
        ringMesh: Mesh,
        textureManager: TextureManager,
        saturn: CelestialBody,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        lightDirection: FloatArray
    ) {
        shader.use()

        val saturnPos = saturn.worldPosition

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, saturnPos.x, saturnPos.y, saturnPos.z)
        Matrix.rotateM(modelMatrix, 0, saturn.config.axialTilt, 0f, 0f, 1f)

        shader.setUniformMatrix4fv("u_modelMatrix", modelMatrix)
        shader.setUniformMatrix4fv("u_viewMatrix", viewMatrix)
        shader.setUniformMatrix4fv("u_projectionMatrix", projectionMatrix)
        shader.setUniform3f("u_planetCenter", saturnPos.x, saturnPos.y, saturnPos.z)
        shader.setUniform1f("u_planetRadius", saturn.config.radius)
        shader.setUniform3f("u_lightDirection", lightDirection[0], lightDirection[1], lightDirection[2])

        textureManager.bind("saturn_ring", 0)
        shader.setUniform1i("u_ringTexture", 0)

        GLES32.glDisable(GLES32.GL_CULL_FACE)
        GLES32.glEnable(GLES32.GL_BLEND)
        GLES32.glBlendFunc(GLES32.GL_SRC_ALPHA, GLES32.GL_ONE_MINUS_SRC_ALPHA)

        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, ringMesh.vboId)
        GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, ringMesh.iboId)

        val stride = ringMesh.stride
        GLES32.glEnableVertexAttribArray(0)
        GLES32.glVertexAttribPointer(0, 3, GLES32.GL_FLOAT, false, stride, 0)
        GLES32.glEnableVertexAttribArray(1)
        GLES32.glVertexAttribPointer(1, 2, GLES32.GL_FLOAT, false, stride, 3 * 4)

        GLES32.glDrawElements(GLES32.GL_TRIANGLES, ringMesh.indexCount, GLES32.GL_UNSIGNED_INT, 0)

        GLES32.glDisableVertexAttribArray(0)
        GLES32.glDisableVertexAttribArray(1)

        GLES32.glDisable(GLES32.GL_BLEND)
        GLES32.glEnable(GLES32.GL_CULL_FACE)
    }
}

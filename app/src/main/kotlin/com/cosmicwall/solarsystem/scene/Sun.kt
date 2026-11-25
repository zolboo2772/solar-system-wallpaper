package com.cosmicwall.solarsystem.scene

import android.opengl.GLES32
import android.opengl.Matrix
import com.cosmicwall.solarsystem.gl.Mesh
import com.cosmicwall.solarsystem.gl.ShaderProgram
import com.cosmicwall.solarsystem.gl.TextureManager
import com.cosmicwall.solarsystem.util.Vector3

class Sun {
    val position = Vector3.ZERO
    val radius = 0.6f
    private var rotationAngle = 0f
    private val modelMatrix = FloatArray(16)

    fun update(deltaTime: Float) {
        rotationAngle += deltaTime * 5f
        if (rotationAngle > 360f) rotationAngle -= 360f
    }

    fun draw(
        shader: ShaderProgram,
        sphereMesh: Mesh,
        textureManager: TextureManager,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        cameraPosition: Vector3,
        time: Float
    ) {
        shader.use()

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.scaleM(modelMatrix, 0, radius, radius, radius)
        Matrix.rotateM(modelMatrix, 0, rotationAngle, 0f, 1f, 0f)

        shader.setUniformMatrix4fv("u_modelMatrix", modelMatrix)
        shader.setUniformMatrix4fv("u_viewMatrix", viewMatrix)
        shader.setUniformMatrix4fv("u_projectionMatrix", projectionMatrix)
        shader.setUniform1f("u_time", time)
        shader.setUniform3f("u_cameraPosition", cameraPosition.x, cameraPosition.y, cameraPosition.z)

        textureManager.bind("sun_diffuse", 0)
        shader.setUniform1i("u_diffuseMap", 0)

        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, sphereMesh.vboId)
        GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, sphereMesh.iboId)

        val stride = sphereMesh.stride
        GLES32.glEnableVertexAttribArray(0)
        GLES32.glVertexAttribPointer(0, 3, GLES32.GL_FLOAT, false, stride, 0)
        GLES32.glEnableVertexAttribArray(1)
        GLES32.glVertexAttribPointer(1, 3, GLES32.GL_FLOAT, false, stride, 3 * 4)
        GLES32.glEnableVertexAttribArray(2)
        GLES32.glVertexAttribPointer(2, 2, GLES32.GL_FLOAT, false, stride, 6 * 4)
        GLES32.glEnableVertexAttribArray(3)
        GLES32.glVertexAttribPointer(3, 4, GLES32.GL_FLOAT, false, stride, 8 * 4)

        GLES32.glDrawElements(GLES32.GL_TRIANGLES, sphereMesh.indexCount, GLES32.GL_UNSIGNED_INT, 0)

        GLES32.glDisableVertexAttribArray(0)
        GLES32.glDisableVertexAttribArray(1)
        GLES32.glDisableVertexAttribArray(2)
        GLES32.glDisableVertexAttribArray(3)
    }
}

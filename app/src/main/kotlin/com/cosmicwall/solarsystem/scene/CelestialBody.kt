package com.cosmicwall.solarsystem.scene

import android.opengl.GLES32
import android.opengl.Matrix
import com.cosmicwall.solarsystem.gl.Mesh
import com.cosmicwall.solarsystem.gl.ShaderProgram
import com.cosmicwall.solarsystem.gl.TextureManager
import com.cosmicwall.solarsystem.util.Vector3
import kotlin.math.cos
import kotlin.math.sin

data class PlanetConfig(
    val name: String,
    val radius: Float,
    val orbitRadius: Float,
    val orbitPeriod: Float,
    val rotationPeriod: Float,
    val axialTilt: Float,
    val diffuseTexture: String,
    val normalTexture: String? = null,
    val nightTexture: String? = null,
    val cloudTexture: String? = null,
    val atmosphereColor: FloatArray? = null,
    val atmosphereIntensity: Float = 0f,
    val roughness: Float = 0.8f,
    val metallic: Float = 0.0f
)

class CelestialBody(val config: PlanetConfig) {
    var worldPosition = Vector3.ZERO
        private set
    var orbitAngle = 0f
    var rotationAngle = 0f

    private val modelMatrix = FloatArray(16)
    private val normalMatrix = FloatArray(9)

    fun update(deltaTime: Float, speedMultiplier: Float) {
        orbitAngle += (deltaTime / config.orbitPeriod) * 360f * speedMultiplier
        if (orbitAngle > 360f) orbitAngle -= 360f

        val rad = Math.toRadians(orbitAngle.toDouble())
        worldPosition = Vector3(
            (config.orbitRadius * cos(rad)).toFloat(),
            0f,
            (config.orbitRadius * sin(rad)).toFloat()
        )

        rotationAngle += (deltaTime / config.rotationPeriod) * 360f * speedMultiplier
        if (rotationAngle > 360f) rotationAngle -= 360f
    }

    fun draw(
        shader: ShaderProgram,
        sphereMesh: Mesh,
        textureManager: TextureManager,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        cameraPosition: Vector3,
        lightDirection: FloatArray,
        time: Float
    ) {
        shader.use()

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, worldPosition.x, worldPosition.y, worldPosition.z)
        Matrix.rotateM(modelMatrix, 0, config.axialTilt, 0f, 0f, 1f)
        Matrix.rotateM(modelMatrix, 0, rotationAngle, 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, config.radius, config.radius, config.radius)

        extractNormalMatrix(modelMatrix, normalMatrix)

        shader.setUniformMatrix4fv("u_modelMatrix", modelMatrix)
        shader.setUniformMatrix4fv("u_viewMatrix", viewMatrix)
        shader.setUniformMatrix4fv("u_projectionMatrix", projectionMatrix)
        GLES32.glUniformMatrix3fv(shader.getUniformLocation("u_normalMatrix"), 1, false, normalMatrix, 0)
        shader.setUniform3f("u_cameraPosition", cameraPosition.x, cameraPosition.y, cameraPosition.z)
        shader.setUniform3f("u_lightDirection", lightDirection[0], lightDirection[1], lightDirection[2])
        shader.setUniform3f("u_lightColor", 1f, 0.98f, 0.92f)
        shader.setUniform1f("u_roughness", config.roughness)
        shader.setUniform1f("u_metallic", config.metallic)
        shader.setUniform1f("u_ambientStrength", 0.04f)

        textureManager.bind(config.diffuseTexture, 0)
        shader.setUniform1i("u_diffuseMap", 0)

        shader.setUniform1f("u_hasNormalMap", if (config.normalTexture != null) 1f else 0f)
        if (config.normalTexture != null) {
            textureManager.bind(config.normalTexture, 1)
            shader.setUniform1i("u_normalMap", 1)
        }

        shader.setUniform1f("u_hasNightMap", if (config.nightTexture != null) 1f else 0f)
        if (config.nightTexture != null) {
            textureManager.bind(config.nightTexture, 2)
            shader.setUniform1i("u_nightMap", 2)
        }

        shader.setUniform1f("u_hasCloudMap", if (config.cloudTexture != null) 1f else 0f)
        if (config.cloudTexture != null) {
            textureManager.bind(config.cloudTexture, 3)
            shader.setUniform1i("u_cloudMap", 3)
            shader.setUniform1f("u_cloudOffset", time * 0.005f)
        }

        if (config.atmosphereColor != null) {
            shader.setUniform3f("u_atmosphereColor",
                config.atmosphereColor[0], config.atmosphereColor[1], config.atmosphereColor[2])
            shader.setUniform1f("u_atmosphereIntensity", config.atmosphereIntensity)
        } else {
            shader.setUniform1f("u_atmosphereIntensity", 0f)
        }

        drawMesh(sphereMesh)
    }

    private fun drawMesh(mesh: Mesh) {
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, mesh.vboId)
        GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, mesh.iboId)

        val stride = mesh.stride
        GLES32.glEnableVertexAttribArray(0)
        GLES32.glVertexAttribPointer(0, 3, GLES32.GL_FLOAT, false, stride, 0)
        GLES32.glEnableVertexAttribArray(1)
        GLES32.glVertexAttribPointer(1, 3, GLES32.GL_FLOAT, false, stride, 3 * 4)
        GLES32.glEnableVertexAttribArray(2)
        GLES32.glVertexAttribPointer(2, 2, GLES32.GL_FLOAT, false, stride, 6 * 4)
        GLES32.glEnableVertexAttribArray(3)
        GLES32.glVertexAttribPointer(3, 4, GLES32.GL_FLOAT, false, stride, 8 * 4)

        GLES32.glDrawElements(GLES32.GL_TRIANGLES, mesh.indexCount, GLES32.GL_UNSIGNED_INT, 0)

        GLES32.glDisableVertexAttribArray(0)
        GLES32.glDisableVertexAttribArray(1)
        GLES32.glDisableVertexAttribArray(2)
        GLES32.glDisableVertexAttribArray(3)
    }

    private fun extractNormalMatrix(modelMatrix: FloatArray, normalMatrix: FloatArray) {
        normalMatrix[0] = modelMatrix[0]; normalMatrix[1] = modelMatrix[1]; normalMatrix[2] = modelMatrix[2]
        normalMatrix[3] = modelMatrix[4]; normalMatrix[4] = modelMatrix[5]; normalMatrix[5] = modelMatrix[6]
        normalMatrix[6] = modelMatrix[8]; normalMatrix[7] = modelMatrix[9]; normalMatrix[8] = modelMatrix[10]
    }

    companion object {
        fun createAllPlanets(): List<CelestialBody> = listOf(
            CelestialBody(PlanetConfig(
                name = "Mercury", radius = 0.08f, orbitRadius = 1.8f,
                orbitPeriod = 15f, rotationPeriod = 58f, axialTilt = 0.03f,
                diffuseTexture = "mercury_diffuse", roughness = 0.95f
            )),
            CelestialBody(PlanetConfig(
                name = "Venus", radius = 0.15f, orbitRadius = 2.6f,
                orbitPeriod = 25f, rotationPeriod = 120f, axialTilt = 177f,
                diffuseTexture = "venus_diffuse",
                atmosphereColor = floatArrayOf(1f, 0.95f, 0.7f),
                atmosphereIntensity = 1.5f, roughness = 0.9f
            )),
            CelestialBody(PlanetConfig(
                name = "Earth", radius = 0.16f, orbitRadius = 3.5f,
                orbitPeriod = 35f, rotationPeriod = 10f, axialTilt = 23.4f,
                diffuseTexture = "earth_diffuse",
                normalTexture = "earth_normal",
                nightTexture = "earth_night",
                cloudTexture = "earth_clouds",
                atmosphereColor = floatArrayOf(0.3f, 0.6f, 1f),
                atmosphereIntensity = 1.2f, roughness = 0.7f
            )),
            CelestialBody(PlanetConfig(
                name = "Mars", radius = 0.11f, orbitRadius = 4.5f,
                orbitPeriod = 50f, rotationPeriod = 10f, axialTilt = 25.2f,
                diffuseTexture = "mars_diffuse",
                atmosphereColor = floatArrayOf(1f, 0.6f, 0.3f),
                atmosphereIntensity = 0.3f, roughness = 0.9f
            )),
            CelestialBody(PlanetConfig(
                name = "Jupiter", radius = 0.45f, orbitRadius = 6.5f,
                orbitPeriod = 80f, rotationPeriod = 4f, axialTilt = 3.1f,
                diffuseTexture = "jupiter_diffuse", roughness = 0.85f
            )),
            CelestialBody(PlanetConfig(
                name = "Saturn", radius = 0.38f, orbitRadius = 8.5f,
                orbitPeriod = 120f, rotationPeriod = 4.5f, axialTilt = 26.7f,
                diffuseTexture = "saturn_diffuse", roughness = 0.85f
            )),
            CelestialBody(PlanetConfig(
                name = "Uranus", radius = 0.22f, orbitRadius = 11.0f,
                orbitPeriod = 180f, rotationPeriod = 7f, axialTilt = 97.8f,
                diffuseTexture = "uranus_diffuse",
                atmosphereColor = floatArrayOf(0.4f, 0.8f, 0.9f),
                atmosphereIntensity = 0.6f, roughness = 0.8f
            )),
            CelestialBody(PlanetConfig(
                name = "Neptune", radius = 0.20f, orbitRadius = 13.5f,
                orbitPeriod = 250f, rotationPeriod = 6.5f, axialTilt = 28.3f,
                diffuseTexture = "neptune_diffuse",
                atmosphereColor = floatArrayOf(0.2f, 0.4f, 1f),
                atmosphereIntensity = 0.8f, roughness = 0.8f
            ))
        )
    }
}

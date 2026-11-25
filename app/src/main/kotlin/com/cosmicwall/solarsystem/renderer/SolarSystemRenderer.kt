package com.cosmicwall.solarsystem.renderer

import android.content.Context
import android.content.SharedPreferences
import android.opengl.GLES32
import android.opengl.GLSurfaceView
import android.util.Log
import com.cosmicwall.solarsystem.data.WallpaperPreferences
import com.cosmicwall.solarsystem.gl.Mesh
import com.cosmicwall.solarsystem.gl.MeshGenerator
import com.cosmicwall.solarsystem.gl.ShaderManager
import com.cosmicwall.solarsystem.gl.TextureManager
import com.cosmicwall.solarsystem.interaction.CameraController
import com.cosmicwall.solarsystem.interaction.RayCaster
import com.cosmicwall.solarsystem.scene.CelestialBody
import com.cosmicwall.solarsystem.scene.SaturnRings
import com.cosmicwall.solarsystem.scene.Skybox
import com.cosmicwall.solarsystem.scene.Sun
import com.cosmicwall.solarsystem.util.FrameRateController
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class SolarSystemRenderer(
    private val context: Context,
    val camera: CameraController,
    val rayCaster: RayCaster
) : GLSurfaceView.Renderer, SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var shaderManager: ShaderManager
    private lateinit var textureManager: TextureManager
    private lateinit var prefs: WallpaperPreferences

    private lateinit var sphereMesh: Mesh
    private lateinit var ringMesh: Mesh

    val sun = Sun()
    val planets = CelestialBody.createAllPlanets()
    private val saturnRings = SaturnRings()
    private val skybox = Skybox()

    private val frameRateController = FrameRateController()
    private var totalTime = 0f
    private val lightDirection = floatArrayOf(-1f, -0.3f, -0.5f)

    var activePlanetName: String? = null
    var labelAlpha = 0f
    private var labelTimer = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "Surface created - initializing GL resources")

        GLES32.glClearColor(0f, 0f, 0f, 1f)
        GLES32.glEnable(GLES32.GL_DEPTH_TEST)
        GLES32.glDepthFunc(GLES32.GL_LEQUAL)
        GLES32.glFrontFace(GLES32.GL_CCW)
        GLES32.glEnable(GLES32.GL_CULL_FACE)
        GLES32.glCullFace(GLES32.GL_BACK)

        prefs = WallpaperPreferences(context)
        prefs.registerListener(this)

        shaderManager = ShaderManager(context)
        shaderManager.compileAll()

        textureManager = TextureManager(context)
        textureManager.loadAll()

        sphereMesh = MeshGenerator.generateSphere(
            prefs.sphereSegmentsLat,
            prefs.sphereSegmentsLon
        )
        ringMesh = MeshGenerator.generateRingDisk(
            innerRadius = 0.5f,
            outerRadius = 1.1f,
            segments = 128
        )

        skybox.init()

        planets.forEachIndexed { index, planet ->
            planet.orbitAngle = (index * 45f) + (Math.random().toFloat() * 30f)
        }

        Log.d(TAG, "GL initialization complete")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        val w = if (width <= 0) 1 else width
        val h = if (height <= 0) 1 else height
        GLES32.glViewport(0, 0, w, h)
        camera.updateProjection(w, h)
        rayCaster.setViewport(w, h)
    }

    override fun onDrawFrame(gl: GL10?) {
        val dt = frameRateController.update()
        totalTime += dt

        val speedMult = prefs.orbitSpeedMultiplier
        camera.isAutoRotating = prefs.autoRotate
        camera.update(dt)
        sun.update(dt)
        planets.forEach { it.update(dt, speedMult) }

        if (activePlanetName != null) {
            labelAlpha = (labelAlpha + dt * 3f).coerceAtMost(1f)
            labelTimer += dt
            if (labelTimer > 3f) {
                labelAlpha = (labelAlpha - dt * 2f).coerceAtLeast(0f)
                if (labelAlpha <= 0f) {
                    activePlanetName = null
                    labelTimer = 0f
                }
            }
        }

        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT or GLES32.GL_DEPTH_BUFFER_BIT)

        val viewMatrix = camera.viewMatrix
        val projMatrix = camera.projectionMatrix
        val camPos = camera.position

        shaderManager.get("skybox")?.let { skyboxShader ->
            skybox.draw(skyboxShader, viewMatrix, projMatrix, prefs.starDensity, totalTime)
        }

        shaderManager.get("sun")?.let { sunShader ->
            sun.draw(sunShader, sphereMesh, textureManager, viewMatrix, projMatrix, camPos, totalTime)
        }

        shaderManager.get("planet")?.let { planetShader ->
            for (planet in planets) {
                val toSun = (sun.position - planet.worldPosition).normalized()
                lightDirection[0] = -toSun.x
                lightDirection[1] = -toSun.y
                lightDirection[2] = -toSun.z

                planet.draw(
                    planetShader, sphereMesh, textureManager,
                    viewMatrix, projMatrix, camPos, lightDirection, totalTime
                )
            }
        }

        val saturn = planets.find { it.config.name == "Saturn" }
        if (saturn != null) {
            shaderManager.get("ring")?.let { ringShader ->
                val toSun = (sun.position - saturn.worldPosition).normalized()
                val saturnLightDir = floatArrayOf(-toSun.x, -toSun.y, -toSun.z)
                saturnRings.draw(
                    ringShader, ringMesh, textureManager,
                    saturn, viewMatrix, projMatrix, saturnLightDir
                )
            }
        }
    }

    fun showPlanetLabel(planet: CelestialBody) {
        activePlanetName = planet.config.name
        labelAlpha = 0f
        labelTimer = 0f
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        Log.d(TAG, "Preference changed: $key")
    }

    fun release() {
        prefs.unregisterListener(this)
        shaderManager.releaseAll()
        textureManager.releaseAll()
        skybox.release()
    }

    companion object {
        private const val TAG = "SolarSystemRenderer"
    }
}

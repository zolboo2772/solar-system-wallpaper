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

    @Volatile
    private var pendingPlanetUpdate = false

    @Volatile
    private var pendingUnlockZoom = false

    private var lastAppliedPlanet: String? = null

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
            innerRadius = 0.486f,
            outerRadius = 0.931f,
            segments = 160
        )

        skybox.init()

        planets.forEachIndexed { index, planet ->
            planet.orbitAngle = (index * 45f) + (Math.random().toFloat() * 30f)
            planet.update(0f, 1f)
        }

        applySelectedPlanet(triggerZoom = prefs.unlockZoomEffect)

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
        sun.update(dt)
        planets.forEach { it.update(dt, speedMult) }

        if (pendingPlanetUpdate || (::prefs.isInitialized && prefs.selectedPlanet != lastAppliedPlanet)) {
            pendingPlanetUpdate = false
            applySelectedPlanet(triggerZoom = true)
        }
        if (pendingUnlockZoom) {
            pendingUnlockZoom = false
            if (::prefs.isInitialized && prefs.unlockZoomEffect) {
                camera.triggerXiaomiUnlockZoom()
            }
        }

        updateHeroCameraTracking()
        camera.update(dt)

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

    fun selectPlanet(name: String) {
        if (::prefs.isInitialized) {
            prefs.selectedPlanet = name
        }
        pendingPlanetUpdate = true
    }

    fun onWallpaperUnlocked() {
        pendingUnlockZoom = true
    }

    private fun applySelectedPlanet(triggerZoom: Boolean) {
        val selected = if (::prefs.isInitialized) prefs.selectedPlanet else "Earth"
        lastAppliedPlanet = selected
        when {
            selected.equals("all", ignoreCase = true) -> {
                camera.setSystemMode()
                camera.snapToTarget(sun.position)
            }
            selected.equals("Sun", ignoreCase = true) -> {
                camera.setHeroMode(distance = 2.5f, elevation = 12f)
                camera.snapToTarget(sun.position)
            }
            else -> {
                val target = planets.find { it.config.name.equals(selected, ignoreCase = true) }
                if (target != null) {
                    val heroDist = if (target.config.name == "Saturn") {
                        3.5f
                    } else {
                        target.config.radius * 4.0f
                    }
                    // Position camera ~65 deg off the Sun-planet line for dramatic crescent terminator lighting
                    val sunAngleDeg = Math.toDegrees(
                        kotlin.math.atan2(target.worldPosition.x.toDouble(), target.worldPosition.z.toDouble())
                    ).toFloat()
                    camera.setHeroMode(distance = heroDist, elevation = 14f, azimuthHint = sunAngleDeg + 115f)
                    camera.snapToTarget(target.worldPosition)
                } else {
                    camera.setSystemMode()
                    camera.snapToTarget(sun.position)
                }
            }
        }
        if (triggerZoom) {
            camera.triggerXiaomiUnlockZoom()
        }
    }

    private fun updateHeroCameraTracking() {
        if (!::prefs.isInitialized) return
        val selected = prefs.selectedPlanet
        if (selected.equals("all", ignoreCase = true)) return
        if (selected.equals("Sun", ignoreCase = true)) {
            camera.trackTarget(sun.position)
            return
        }
        val target = planets.find { it.config.name.equals(selected, ignoreCase = true) }
        if (target != null) {
            camera.trackTarget(target.worldPosition)
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        Log.d(TAG, "Preference changed: $key")
        if (key == "selected_planet") {
            pendingPlanetUpdate = true
        }
    }

    fun release() {
        if (::prefs.isInitialized) prefs.unregisterListener(this)
        if (::shaderManager.isInitialized) shaderManager.releaseAll()
        if (::textureManager.isInitialized) textureManager.releaseAll()
        if (::sphereMesh.isInitialized) sphereMesh.release()
        if (::ringMesh.isInitialized) ringMesh.release()
        skybox.release()
    }

    companion object {
        private const val TAG = "SolarSystemRenderer"
    }
}

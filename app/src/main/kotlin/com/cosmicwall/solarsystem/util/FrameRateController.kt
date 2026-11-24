package com.cosmicwall.solarsystem.util

class FrameRateController {
    private var lastFrameTime = System.nanoTime()
    private var deltaTime = 0f
    private var frameCount = 0
    private var fpsTimer = 0f
    var currentFps = 0f
        private set

    var targetFps = 60
    var isPowerSaveMode = false

    fun update(): Float {
        val now = System.nanoTime()
        deltaTime = (now - lastFrameTime) / 1_000_000_000f
        lastFrameTime = now

        deltaTime = deltaTime.coerceAtMost(0.1f)

        frameCount++
        fpsTimer += deltaTime
        if (fpsTimer >= 1f) {
            currentFps = frameCount / fpsTimer
            frameCount = 0
            fpsTimer = 0f
        }

        return deltaTime
    }

    fun getEffectiveTargetFps(): Int {
        return if (isPowerSaveMode) 30 else targetFps
    }

    fun getFrameIntervalMs(): Long {
        return 1000L / getEffectiveTargetFps()
    }
}

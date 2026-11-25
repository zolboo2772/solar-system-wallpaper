package com.cosmicwall.solarsystem.gl

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Generates seamless 3D-mapped procedural planetary textures using fractional Brownian motion (fBm)
 * evaluated on the unit sphere to eliminate UV seam and polar-pinching artifacts.
 */
object ProceduralTextureGenerator {

    private const val WIDTH = 512
    private const val HEIGHT = 256

    // Precomputed unit-sphere coordinates for the 512x256 equirectangular grid
    private val sphereX: FloatArray
    private val sphereY: FloatArray
    private val sphereZ: FloatArray
    private val latGrid: FloatArray
    private val lonGrid: FloatArray

    private var cachedEarthHeight: FloatArray? = null

    init {
        val total = WIDTH * HEIGHT
        sphereX = FloatArray(total)
        sphereY = FloatArray(total)
        sphereZ = FloatArray(total)
        latGrid = FloatArray(HEIGHT)
        lonGrid = FloatArray(WIDTH)

        val cosLon = FloatArray(WIDTH)
        val sinLon = FloatArray(WIDTH)
        for (x in 0 until WIDTH) {
            val lon = (x.toFloat() / WIDTH) * (2.0 * Math.PI)
            lonGrid[x] = lon.toFloat()
            cosLon[x] = cos(lon).toFloat()
            sinLon[x] = sin(lon).toFloat()
        }

        for (y in 0 until HEIGHT) {
            val v = y.toFloat() / (HEIGHT - 1)
            val theta = v * Math.PI // 0 at north pole, PI at south pole
            val sinTheta = sin(theta).toFloat()
            val cosTheta = cos(theta).toFloat()
            latGrid[y] = (0.5 * Math.PI - theta).toFloat() // +PI/2 north, -PI/2 south

            val rowOffset = y * WIDTH
            for (x in 0 until WIDTH) {
                val idx = rowOffset + x
                sphereX[idx] = sinTheta * cosLon[x]
                sphereY[idx] = cosTheta
                sphereZ[idx] = sinTheta * sinLon[x]
            }
        }
    }

    fun generate(name: String): Bitmap {
        return when {
            name.contains("sun") -> generateSun()
            name.contains("mercury") -> generateMercury()
            name.contains("venus") -> generateVenus()
            name.contains("earth_diffuse") -> generateEarthDiffuse()
            name.contains("earth_normal") -> generateEarthNormal()
            name.contains("earth_night") -> generateEarthNight()
            name.contains("earth_clouds") -> generateEarthClouds()
            name.contains("mars") -> generateMars()
            name.contains("jupiter") -> generateJupiter()
            name.contains("saturn_ring") -> generateSaturnRing()
            name.contains("saturn") -> generateSaturn()
            name.contains("uranus") -> generateUranus()
            name.contains("neptune") -> generateNeptune()
            else -> Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.GRAY)
            }
        }
    }

    private fun getEarthHeightMap(): FloatArray {
        cachedEarthHeight?.let { return it }
        val total = WIDTH * HEIGHT
        val heightMap = FloatArray(total)
        for (i in 0 until total) {
            val x = sphereX[i]
            val y = sphereY[i]
            val z = sphereZ[i]
            // Domain warp for realistic tectonic coastlines
            val wx = x + 0.35f * fbm3(x * 2.2f + 3.1f, y * 2.2f, z * 2.2f, 3, 17)
            val wy = y + 0.35f * fbm3(x * 2.2f, y * 2.2f + 7.4f, z * 2.2f, 3, 29)
            val wz = z + 0.35f * fbm3(x * 2.2f, y * 2.2f, z * 2.2f + 5.2f, 3, 43)

            val continents = fbm3(wx * 2.1f, wy * 2.1f, wz * 2.1f, 5, 101)
            val detail = fbm3(x * 8.5f, y * 8.5f, z * 8.5f, 3, 211) * 0.18f
            // Shift so ~32% is land above 0.0
            heightMap[i] = (continents + detail) - 0.04f
        }
        cachedEarthHeight = heightMap
        return heightMap
    }

    private fun generateEarthDiffuse(): Bitmap {
        val hMap = getEarthHeightMap()
        val pixels = IntArray(WIDTH * HEIGHT)

        for (y in 0 until HEIGHT) {
            val absLat = abs(latGrid[y]) / (Math.PI.toFloat() * 0.5f) // 0 equator -> 1 pole
            val rowOffset = y * WIDTH
            for (x in 0 until WIDTH) {
                val idx = rowOffset + x
                val h = hMap[idx]
                val nx = sphereX[idx]
                val ny = sphereY[idx]
                val nz = sphereZ[idx]

                // Polar ice cap threshold
                val iceNoise = fbm3(nx * 6f, ny * 6f, nz * 6f, 2, 307) * 0.08f
                if (absLat + iceNoise > 0.84f) {
                    pixels[idx] = rgb(236, 244, 252)
                    continue
                }

                if (h <= 0f) {
                    // Ocean bathymetry: shallow coastal turquoise -> deep abyssal blue
                    val depth = (-h * 3.5f).coerceIn(0f, 1f)
                    val r = lerp(24f, 6f, depth)
                    val g = lerp(118f, 34f, depth)
                    val b = lerp(175f, 96f, depth)
                    pixels[idx] = rgb(r, g, b)
                } else {
                    // Land biomes based on latitude, moisture noise, and elevation
                    val elev = (h * 2.4f).coerceIn(0f, 1f)
                    val moisture = fbm3(nx * 4.5f + 12f, ny * 4.5f, nz * 4.5f, 3, 409) * 0.5f + 0.5f
                    val aridity = (1f - abs(absLat - 0.28f) * 2.5f).coerceIn(0f, 1f) * (1f - moisture)

                    // Base vegetation vs desert
                    var r = lerp(38f, 188f, aridity)
                    var g = lerp(108f, 158f, aridity)
                    var b = lerp(46f, 102f, aridity)

                    // High mountains -> rocky grey-brown -> snow
                    if (elev > 0.45f) {
                        val m = ((elev - 0.45f) / 0.35f).coerceIn(0f, 1f)
                        r = lerp(r, 128f, m)
                        g = lerp(g, 118f, m)
                        b = lerp(b, 108f, m)
                    }
                    if (elev > 0.78f) {
                        val s = ((elev - 0.78f) / 0.22f).coerceIn(0f, 1f)
                        r = lerp(r, 240f, s)
                        g = lerp(g, 245f, s)
                        b = lerp(b, 250f, s)
                    }
                    pixels[idx] = rgb(r, g, b)
                }
            }
        }
        return bitmapFromPixels(pixels, WIDTH, HEIGHT)
    }

    private fun generateEarthNormal(): Bitmap {
        val hMap = getEarthHeightMap()
        val pixels = IntArray(WIDTH * HEIGHT)
        val strength = 14f

        for (y in 0 until HEIGHT) {
            val yUp = max(0, y - 1)
            val yDown = min(HEIGHT - 1, y + 1)
            val rowOffset = y * WIDTH
            for (x in 0 until WIDTH) {
                val idx = rowOffset + x
                if (hMap[idx] <= 0f) {
                    // Smooth ocean normal
                    pixels[idx] = rgb(128f, 128f, 255f)
                    continue
                }
                val xLeft = (x - 1 + WIDTH) % WIDTH
                val xRight = (x + 1) % WIDTH

                val hL = max(0f, hMap[rowOffset + xLeft])
                val hR = max(0f, hMap[rowOffset + xRight])
                val hU = max(0f, hMap[yUp * WIDTH + x])
                val hD = max(0f, hMap[yDown * WIDTH + x])

                val dx = (hL - hR) * strength
                val dy = (hU - hD) * strength
                val dz = 1f
                val invLen = 1f / sqrt(dx * dx + dy * dy + dz * dz)

                val nr = (dx * invLen * 0.5f + 0.5f) * 255f
                val ng = (dy * invLen * 0.5f + 0.5f) * 255f
                val nb = (dz * invLen * 0.5f + 0.5f) * 255f
                pixels[idx] = rgb(nr, ng, nb)
            }
        }
        return bitmapFromPixels(pixels, WIDTH, HEIGHT)
    }

    private fun generateEarthNight(): Bitmap {
        val hMap = getEarthHeightMap()
        val pixels = IntArray(WIDTH * HEIGHT)

        for (y in 0 until HEIGHT) {
            val absLat = abs(latGrid[y]) / (Math.PI.toFloat() * 0.5f)
            val rowOffset = y * WIDTH
            for (x in 0 until WIDTH) {
                val idx = rowOffset + x
                val h = hMap[idx]
                if (h <= 0.005f || h > 0.35f || absLat > 0.72f) {
                    pixels[idx] = Color.BLACK
                    continue
                }
                val nx = sphereX[idx]
                val ny = sphereY[idx]
                val nz = sphereZ[idx]

                val region = fbm3(nx * 6f, ny * 6f, nz * 6f, 3, 503) * 0.5f + 0.5f
                val sparks = valueNoise3(nx * 48f, ny * 48f, nz * 48f, 521)
                val coastalBoost = (1f - (h / 0.12f)).coerceIn(0f, 1f) * 0.35f

                val city = ((region * 0.7f + coastalBoost) * sparks - 0.38f) * 4.5f
                val intensity = city.coerceIn(0f, 1f).pow(1.4f)
                if (intensity < 0.03f) {
                    pixels[idx] = Color.BLACK
                } else {
                    pixels[idx] = rgb(255f * intensity, 200f * intensity, 105f * intensity)
                }
            }
        }
        return bitmapFromPixels(pixels, WIDTH, HEIGHT)
    }

    private fun generateEarthClouds(): Bitmap {
        val pixels = IntArray(WIDTH * HEIGHT)
        for (i in 0 until WIDTH * HEIGHT) {
            val nx = sphereX[i]
            val ny = sphereY[i]
            val nz = sphereZ[i]

            // Cyclonic swirl warp
            val swirl = fbm3(nx * 3f, ny * 3f, nz * 3f, 3, 601)
            val c = fbm3(
                nx * 4.2f + swirl * 0.7f,
                ny * 5.0f,
                nz * 4.2f - swirl * 0.7f,
                4,
                619
            ) * 0.5f + 0.5f

            val cloud = ((c - 0.44f) / 0.40f).coerceIn(0f, 1f)
            val v = (cloud * 255f).toInt().coerceIn(0, 255)
            // Planet shader reads .r from cloudMap
            pixels[i] = Color.argb(255, v, v, v)
        }
        return bitmapFromPixels(pixels, WIDTH, HEIGHT)
    }

    private fun generateSun(): Bitmap {
        val pixels = IntArray(WIDTH * HEIGHT)
        for (y in 0 until HEIGHT) {
            val absLat = abs(latGrid[y]) / (Math.PI.toFloat() * 0.5f)
            val rowOffset = y * WIDTH
            for (x in 0 until WIDTH) {
                val idx = rowOffset + x
                val nx = sphereX[idx]
                val ny = sphereY[idx]
                val nz = sphereZ[idx]

                val gran = fbm3(nx * 14f, ny * 14f, nz * 14f, 4, 701) * 0.5f + 0.5f
                val active = fbm3(nx * 4f, ny * 4f, nz * 4f, 3, 719) * 0.5f + 0.5f

                // Sunspots in mid-low latitudes
                var spot = 0f
                if (absLat < 0.45f) {
                    val sp = valueNoise3(nx * 18f, ny * 18f, nz * 18f, 733)
                    if (sp > 0.84f) {
                        spot = ((sp - 0.84f) / 0.16f).coerceIn(0f, 1f)
                    }
                }

                val heat = (gran * 0.65f + active * 0.35f - spot * 0.75f).coerceIn(0f, 1f)
                val r = lerp(180f, 255f, heat)
                val g = lerp(65f, 232f, heat.pow(1.2f))
                val b = lerp(10f, 130f, heat.pow(2.0f))
                pixels[idx] = rgb(r, g, b)
            }
        }
        return bitmapFromPixels(pixels, WIDTH, HEIGHT)
    }

    private fun generateMercury(): Bitmap {
        val pixels = IntArray(WIDTH * HEIGHT)
        // Generate deterministic crater centers on unit sphere
        val craterCount = 55
        val cx = FloatArray(craterCount)
        val cy = FloatArray(craterCount)
        val cz = FloatArray(craterCount)
        val cr = FloatArray(craterCount)
        for (c in 0 until craterCount) {
            val u = hash1(c * 13 + 1) * 2f - 1f
            val phi = hash1(c * 29 + 7) * 2f * Math.PI.toFloat()
            val s = sqrt(max(0f, 1f - u * u))
            cx[c] = s * cos(phi)
            cy[c] = u
            cz[c] = s * sin(phi)
            cr[c] = 0.04f + hash1(c * 41 + 3).pow(2f) * 0.18f
        }

        for (i in 0 until WIDTH * HEIGHT) {
            val nx = sphereX[i]
            val ny = sphereY[i]
            val nz = sphereZ[i]

            var albedo = 0.52f + fbm3(nx * 5f, ny * 5f, nz * 5f, 4, 809) * 0.25f
            albedo += fbm3(nx * 18f, ny * 18f, nz * 18f, 2, 823) * 0.08f

            for (c in 0 until craterCount) {
                val dx = nx - cx[c]
                val dy = ny - cy[c]
                val dz = nz - cz[c]
                val d2 = dx * dx + dy * dy + dz * dz
                val rad = cr[c]
                if (d2 < rad * rad * 1.5f) {
                    val d = sqrt(d2) / rad
                    if (d < 0.78f) {
                        albedo -= 0.14f * (1f - d / 0.78f)
                    } else if (d < 1.15f) {
                        val rim = 1f - abs(d - 0.92f) / 0.23f
                        albedo += 0.16f * max(0f, rim)
                    }
                }
            }

            val v = albedo.coerceIn(0.18f, 0.88f)
            pixels[i] = rgb(v * 235f, v * 228f, v * 220f)
        }
        return bitmapFromPixels(pixels, WIDTH, HEIGHT)
    }

    private fun generateVenus(): Bitmap {
        val pixels = IntArray(WIDTH * HEIGHT)
        for (y in 0 until HEIGHT) {
            val lat = latGrid[y] // -PI/2..PI/2
            val rowOffset = y * WIDTH
            for (x in 0 until WIDTH) {
                val idx = rowOffset + x
                val nx = sphereX[idx]
                val ny = sphereY[idx]
                val nz = sphereZ[idx]

                // Super-rotation chevron pattern
                val warp = fbm3(nx * 2.5f, ny * 4.0f, nz * 2.5f, 3, 907)
                val band = sin((lat * 7.0f + warp * 2.8f).toDouble()).toFloat() * 0.5f + 0.5f
                val cloud = fbm3(nx * 4.5f + warp * 0.6f, ny * 8.0f, nz * 4.5f, 4, 919) * 0.5f + 0.5f

                val t = (band * 0.45f + cloud * 0.55f).coerceIn(0f, 1f)
                val r = lerp(206f, 246f, t)
                val g = lerp(164f, 224f, t)
                val b = lerp(104f, 178f, t)
                pixels[idx] = rgb(r, g, b)
            }
        }
        return bitmapFromPixels(pixels, WIDTH, HEIGHT)
    }

    private fun generateMars(): Bitmap {
        val pixels = IntArray(WIDTH * HEIGHT)
        for (y in 0 until HEIGHT) {
            val lat = latGrid[y]
            val absLat = abs(lat) / (Math.PI.toFloat() * 0.5f)
            val rowOffset = y * WIDTH
            for (x in 0 until WIDTH) {
                val idx = rowOffset + x
                val nx = sphereX[idx]
                val ny = sphereY[idx]
                val nz = sphereZ[idx]

                // Polar ice caps
                val iceNoise = fbm3(nx * 8f, ny * 8f, nz * 8f, 2, 1009) * 0.05f
                if (absLat + iceNoise > 0.89f) {
                    pixels[idx] = rgb(242, 238, 235)
                    continue
                }

                val terrain = fbm3(nx * 3.0f, ny * 3.0f, nz * 3.0f, 5, 1021) * 0.5f + 0.5f
                val fine = fbm3(nx * 14f, ny * 14f, nz * 14f, 3, 1039) * 0.14f

                // Valles Marineris equatorial canyon scar
                var canyon = 0f
                if (abs(lat + 0.12f) < 0.09f && nx > 0.2f && nz > -0.3f && nz < 0.5f) {
                    canyon = 0.28f * (1f - abs(lat + 0.12f) / 0.09f)
                }

                val t = (terrain + fine - canyon).coerceIn(0f, 1f)
                val r = lerp(98f, 224f, t)
                val g = lerp(42f, 114f, t)
                val b = lerp(26f, 64f, t)
                pixels[idx] = rgb(r, g, b)
            }
        }
        return bitmapFromPixels(pixels, WIDTH, HEIGHT)
    }

    private fun generateJupiter(): Bitmap {
        val pixels = IntArray(WIDTH * HEIGHT)
        // Great Red Spot center around lat -22 deg (-0.38 rad), lon 120 deg (2.1 rad)
        val grsLat = -0.38f
        val grsLon = 2.1f

        for (y in 0 until HEIGHT) {
            val lat = latGrid[y]
            val rowOffset = y * WIDTH
            for (x in 0 until WIDTH) {
                val idx = rowOffset + x
                val lon = lonGrid[x]
                val nx = sphereX[idx]
                val ny = sphereY[idx]
                val nz = sphereZ[idx]

                val turb = fbm3(nx * 5.5f, ny * 11f, nz * 5.5f, 4, 1103) * 0.12f
                val warpedLat = lat + turb

                // Multi-frequency zonal bands
                val b1 = sin((warpedLat * 14f).toDouble()).toFloat()
                val b2 = cos((warpedLat * 27f + 0.8f).toDouble()).toFloat() * 0.4f
                val band = ((b1 + b2) * 0.35f + 0.5f).coerceIn(0f, 1f)

                val fineStreak = fbm3(nx * 16f, ny * 24f, nz * 16f, 2, 1129) * 0.10f
                val t = (band + fineStreak).coerceIn(0f, 1f)

                var r = lerp(152f, 238f, t)
                var g = lerp(96f, 214f, t)
                var b = lerp(62f, 178f, t)

                // Great Red Spot elliptical vortex
                var dLon = abs(lon - grsLon)
                if (dLon > Math.PI) dLon = (2.0 * Math.PI - dLon).toFloat()
                val dLat = (lat - grsLat) / 0.14f
                val dX = dLon / 0.26f
                val grsDist = sqrt(dX * dX + dLat * dLat)
                if (grsDist < 1.0f) {
                    val swirl = 1f - grsDist
                    val ring = sin((grsDist * 10f).toDouble()).toFloat() * 0.15f + 0.85f
                    r = lerp(r, 206f * ring, swirl.pow(0.7f))
                    g = lerp(g, 74f * ring, swirl.pow(0.7f))
                    b = lerp(b, 48f * ring, swirl.pow(0.7f))
                }

                pixels[idx] = rgb(r, g, b)
            }
        }
        return bitmapFromPixels(pixels, WIDTH, HEIGHT)
    }

    private fun generateSaturn(): Bitmap {
        val pixels = IntArray(WIDTH * HEIGHT)
        for (y in 0 until HEIGHT) {
            val lat = latGrid[y]
            val absLat = abs(lat) / (Math.PI.toFloat() * 0.5f)
            val rowOffset = y * WIDTH
            for (x in 0 until WIDTH) {
                val idx = rowOffset + x
                val nx = sphereX[idx]
                val ny = sphereY[idx]
                val nz = sphereZ[idx]

                val turb = fbm3(nx * 4f, ny * 9f, nz * 4f, 3, 1201) * 0.05f
                val band = sin(((lat + turb) * 16f).toDouble()).toFloat() * 0.5f + 0.5f
                val polarDark = (absLat - 0.72f).coerceIn(0f, 0.28f) * 1.4f

                val t = (band * 0.75f + 0.15f).coerceIn(0f, 1f)
                val r = lerp(188f, 236f, t) * (1f - polarDark * 0.35f)
                val g = lerp(164f, 212f, t) * (1f - polarDark * 0.30f)
                val b = lerp(112f, 162f, t) * (1f - polarDark * 0.18f)
                pixels[idx] = rgb(r, g, b)
            }
        }
        return bitmapFromPixels(pixels, WIDTH, HEIGHT)
    }

    private fun generateSaturnRing(): Bitmap {
        val rw = 512
        val rh = 64
        val pixels = IntArray(rw * rh)
        for (x in 0 until rw) {
            val u = x.toFloat() / (rw - 1) // 0 inner -> 1 outer
            var alpha = 0f
            var tint = 0.8f

            when {
                // C-Ring (faint inner crepe ring)
                u < 0.22f -> {
                    alpha = ((u - 0.04f) / 0.18f).coerceIn(0f, 1f) * 0.28f
                    tint = 0.62f
                }
                // B-Ring (brightest, densest, with fine ringlets)
                u < 0.64f -> {
                    val ringlet = sin((u * 140f).toDouble()).toFloat() * 0.12f +
                        cos((u * 310f).toDouble()).toFloat() * 0.06f
                    alpha = (0.86f + ringlet).coerceIn(0.4f, 0.98f)
                    tint = (0.90f + ringlet * 0.5f).coerceIn(0.65f, 1f)
                }
                // Cassini Division (sharp dark gap)
                u < 0.71f -> {
                    alpha = 0.03f
                    tint = 0.4f
                }
                // A-Ring (with Encke Gap around 0.89)
                u < 0.94f -> {
                    if (u > 0.885f && u < 0.902f) {
                        alpha = 0.04f
                    } else {
                        val ringlet = sin((u * 190f).toDouble()).toFloat() * 0.08f
                        alpha = (0.64f + ringlet).coerceIn(0.35f, 0.80f)
                        tint = 0.82f
                    }
                }
                // F-Ring (thin outer filament)
                u > 0.965f && u < 0.982f -> {
                    alpha = 0.45f
                    tint = 0.92f
                }
            }

            val a = (alpha * 255f).toInt().coerceIn(0, 255)
            val r = (228f * tint).toInt().coerceIn(0, 255)
            val g = (208f * tint).toInt().coerceIn(0, 255)
            val b = (172f * tint).toInt().coerceIn(0, 255)
            val col = Color.argb(a, r, g, b)

            for (y in 0 until rh) {
                pixels[y * rw + x] = col
            }
        }
        return bitmapFromPixels(pixels, rw, rh)
    }

    private fun generateUranus(): Bitmap {
        val pixels = IntArray(WIDTH * HEIGHT)
        for (y in 0 until HEIGHT) {
            val lat = latGrid[y]
            val absLat = abs(lat) / (Math.PI.toFloat() * 0.5f)
            val rowOffset = y * WIDTH
            for (x in 0 until WIDTH) {
                val idx = rowOffset + x
                val band = sin((lat * 6f).toDouble()).toFloat() * 0.05f
                val polarBright = (absLat - 0.55f).coerceIn(0f, 0.45f) * 0.25f
                val t = (0.5f + band + polarBright).coerceIn(0f, 1f)
                pixels[idx] = rgb(lerp(132f, 185f, t), lerp(208f, 232f, t), lerp(218f, 238f, t))
            }
        }
        return bitmapFromPixels(pixels, WIDTH, HEIGHT)
    }

    private fun generateNeptune(): Bitmap {
        val pixels = IntArray(WIDTH * HEIGHT)
        for (y in 0 until HEIGHT) {
            val lat = latGrid[y]
            val rowOffset = y * WIDTH
            for (x in 0 until WIDTH) {
                val idx = rowOffset + x
                val lon = lonGrid[x]
                val nx = sphereX[idx]
                val ny = sphereY[idx]
                val nz = sphereZ[idx]

                val turb = fbm3(nx * 4.5f, ny * 8f, nz * 4.5f, 3, 1301) * 0.08f
                val band = sin(((lat + turb) * 10f).toDouble()).toFloat() * 0.5f + 0.5f

                var r = lerp(34f, 64f, band)
                var g = lerp(74f, 128f, band)
                var b = lerp(188f, 242f, band)

                // High-altitude white methane cirrus streaks
                val streak = fbm3(nx * 6f, ny * 18f, nz * 6f, 3, 1327)
                if (streak > 0.36f) {
                    val s = ((streak - 0.36f) / 0.25f).coerceIn(0f, 1f) * 0.75f
                    r = lerp(r, 225f, s)
                    g = lerp(g, 238f, s)
                    b = lerp(b, 255f, s)
                }

                // Great Dark Spot
                var dLon = abs(lon - 3.6f)
                if (dLon > Math.PI) dLon = (2.0 * Math.PI - dLon).toFloat()
                val dLat = (lat + 0.34f) / 0.13f
                val dX = dLon / 0.24f
                val dist = sqrt(dX * dX + dLat * dLat)
                if (dist < 1f) {
                    val f = (1f - dist).pow(0.8f) * 0.55f
                    r *= (1f - f)
                    g *= (1f - f)
                    b *= (1f - f * 0.6f)
                }

                pixels[idx] = rgb(r, g, b)
            }
        }
        return bitmapFromPixels(pixels, WIDTH, HEIGHT)
    }

    // --- Fast 3D Value Noise & fBm ---

    private fun fbm3(x: Float, y: Float, z: Float, octaves: Int, seed: Int): Float {
        var sum = 0f
        var amp = 0.5f
        var freq = 1f
        for (o in 0 until octaves) {
            sum += amp * (valueNoise3(x * freq, y * freq, z * freq, seed + o * 97) * 2f - 1f)
            freq *= 2.03f
            amp *= 0.5f
        }
        return sum
    }

    private fun valueNoise3(x: Float, y: Float, z: Float, seed: Int): Float {
        val ix = floor(x).toInt()
        val iy = floor(y).toInt()
        val iz = floor(z).toInt()
        val fx = x - ix
        val fy = y - iy
        val fz = z - iz

        val ux = fx * fx * (3f - 2f * fx)
        val uy = fy * fy * (3f - 2f * fy)
        val uz = fz * fz * (3f - 2f * fz)

        val n000 = hash3(ix, iy, iz, seed)
        val n100 = hash3(ix + 1, iy, iz, seed)
        val n010 = hash3(ix, iy + 1, iz, seed)
        val n110 = hash3(ix + 1, iy + 1, iz, seed)
        val n001 = hash3(ix, iy, iz + 1, seed)
        val n101 = hash3(ix + 1, iy, iz + 1, seed)
        val n011 = hash3(ix, iy + 1, iz + 1, seed)
        val n111 = hash3(ix + 1, iy + 1, iz + 1, seed)

        val nx00 = n000 + (n100 - n000) * ux
        val nx10 = n010 + (n110 - n010) * ux
        val nx01 = n001 + (n101 - n001) * ux
        val nx11 = n011 + (n111 - n011) * ux

        val nxy0 = nx00 + (nx10 - nx00) * uy
        val nxy1 = nx01 + (nx11 - nx01) * uy

        return nxy0 + (nxy1 - nxy0) * uz
    }

    private fun hash3(x: Int, y: Int, z: Int, seed: Int): Float {
        var h = x * 374761393 + y * 668265263 + z * 2147483647 + seed * 1597334677
        h = (h xor (h ushr 13)) * 1274126177
        h = h xor (h ushr 16)
        return (h and 0x7FFFFFFF) / 2147483647f
    }

    private fun hash1(n: Int): Float {
        var h = n * 1597334677
        h = (h xor (h ushr 13)) * 1274126177
        h = h xor (h ushr 16)
        return (h and 0x7FFFFFFF) / 2147483647f
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun rgb(r: Int, g: Int, b: Int): Int =
        Color.argb(255, r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))

    private fun rgb(r: Float, g: Float, b: Float): Int =
        Color.argb(255, r.toInt().coerceIn(0, 255), g.toInt().coerceIn(0, 255), b.toInt().coerceIn(0, 255))

    private fun bitmapFromPixels(pixels: IntArray, width: Int, height: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.setPixels(pixels, 0, width, 0, 0, width, height)
        return bmp
    }
}

# 🪐 Solar System Planets Live Wallpaper (Samsung Galaxy S25 Ultra)

An interactive, high-fidelity 3D Solar System live wallpaper built in **Kotlin** and **OpenGL ES 3.2** designed specifically for high-refresh-rate AMOLED displays (such as the **Samsung Galaxy S25 Ultra**'s 1–120Hz LTPO display with Snapdragon 8 Elite & Adreno 830 GPU).

---

## ✨ Features

- **Physically-Based Rendering (PBR)**:
  - Custom GLSL ES 3.2 Cook-Torrance BRDF shaders (GGX normal distribution, Schlick-GGX geometry, Fresnel-Schlick).
  - Microfacet specular reflections, surface roughness, and metallic parameters tailored per planet.
- **Dynamic Planetary Atmospheres**:
  - Distance & angle-scaled atmospheric rim glow (Rayleigh/Mie approximation) for Earth (cyan/blue), Venus (dense yellow), Mars (thin amber), Uranus (ice cyan), and Neptune (deep azure).
- **Multi-Layered Earth**:
  - Day diffuse texture + normal map for topographic relief + night-side city lights blended dynamically along the day/night terminator + separate animated UV-scrolling cloud layer.
- **Dynamic Saturn Rings**:
  - Double-sided alpha-blended planar ring disk featuring analytical planetary shadow projection.
- **Procedural Animated Sun & Corona**:
  - Fractional Brownian Motion (fBm) simplex turbulence shaders creating living solar flares and corona rim glow.
- **Infinite Procedural Starfield Skybox**:
  - 3D cellular hash-based starfield with twinkle animation, stellar temperature color distribution (O/B/A/F/G/K/M stars), and subtle galactic nebula haze.
- **Fluid Touch Gestures & Physics**:
  - **Single-finger drag**: Orbit camera around the solar system with momentum damping.
  - **Pinch-to-zoom**: Smooth FOV / orbital distance scaling (clamped 4–30 AU units).
  - **Tap planet**: 3D Ray-casting unprojection to select and smoothly fly into planet orbit + display name label.
  - **Double tap**: Instantly resets to solar system overview.
  - **Home screen scroll**: Dynamic parallax translation via `onOffsetsChanged`.
- **Battery & Thermal Efficiency**:
  - Strict `onVisibilityChanged` pausing prevents background battery drain.
  - V-sync aligned frame rate pacing with 60 FPS standard and automatic throttling to 30 FPS when Power Saving mode is engaged.
  - Embedded procedural texture generation fallback so the app builds and runs immediately even before downloading external 2K/4K texture packages.

---

## 📁 Project Architecture

```
SolarSystemWallpaper/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       │   ├── shaders/
│       │   │   ├── planet.vert / planet.frag   (PBR, Normal, Clouds, Night, Atmosphere)
│       │   │   ├── sun.vert / sun.frag         (fBm noise, corona emission)
│       │   │   ├── ring.vert / ring.frag       (Radial ring UV, planet shadow)
│       │   │   ├── skybox.vert / skybox.frag   (Cellular procedural starfield)
│       │   │   └── label.vert / label.frag     (Screen-space planet UI HUD)
│       │   └── textures/                       (Asset directory for NASA textures)
│       ├── kotlin/com/cosmicwall/solarsystem/
│       │   ├── wallpaper/
│       │   │   ├── SolarSystemWallpaperService.kt (WallpaperService.Engine lifecycle)
│       │   │   └── WallpaperGLSurfaceView.kt   (Custom EGL SurfaceHolder wrapper)
│       │   ├── renderer/
│       │   │   └── SolarSystemRenderer.kt      (GL 3.2 rendering loop)
│       │   ├── scene/
│       │   │   ├── CelestialBody.kt            (Planetary configs, orbits & draw calls)
│       │   │   ├── Sun.kt                      (Solar mesh & lighting origin)
│       │   │   ├── SaturnRings.kt              (Planar ring disk)
│       │   │   └── Skybox.kt                   (Unit skybox mesh)
│       │   ├── gl/
│       │   │   ├── MeshGenerator.kt            (UV spheres & ring geometry)
│       │   │   ├── ShaderProgram.kt            (GL program compiler & uniform binder)
│       │   │   ├── ShaderManager.kt            (Program asset loader & cache)
│       │   │   └── TextureManager.kt           (Bitmap loader + procedural fallback)
│       │   ├── interaction/
│       │   │   ├── CameraController.kt         (Spherical orbit coordinates & damping)
│       │   │   ├── TouchHandler.kt             (Gesture & scale detectors)
│       │   │   └── RayCaster.kt                (Ray-sphere intersection tests)
│       │   ├── ui/
│       │   │   ├── MainActivity.kt             (Launcher with "Set Wallpaper" button)
│       │   │   └── SettingsActivity.kt         (Preference screen)
│       │   ├── data/
│       │   │   └── WallpaperPreferences.kt     (SharedPreferences live listener)
│       │   └── util/
│       │       ├── Vector3.kt                  (3D vector mathematics)
│       │       └── FrameRateController.kt      (Frame delta & FPS calculation)
│       └── res/
│           ├── layout/ (activity_main.xml, activity_settings.xml)
│           ├── xml/ (wallpaper_config.xml, preferences.xml)
│           └── values/ (strings.xml, themes.xml, arrays.xml)
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## 🚀 Building & Running

### 1. Open in Android Studio
1. Launch **Android Studio (Ladybug / Iguana or newer)**.
2. Select **File > Open** and browse to:
   ```
   C:\Users\zolbo\.gemini\antigravity\scratch\SolarSystemWallpaper
   ```
3. Allow Gradle to sync dependencies.

### 2. Build via Command Line
```bash
cd C:\Users\zolbo\.gemini\antigravity\scratch\SolarSystemWallpaper
./gradlew assembleDebug
```
The compiled APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 3. Deploy to Device or Emulator
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
1. Open the **Solar System** app from your launcher.
2. Tap **"Set as Wallpaper"**.
3. Choose **"Home screen and lock screen"**.

---

## 🎨 Adding High-Resolution NASA Textures (Optional)

The application includes built-in procedural texture generators for instant out-of-the-box operation. To use photographic NASA 2K or 4K textures:
1. Download equirectangular planetary maps (e.g. from [Solar System Scope](https://www.solarsystemscope.com/textures/)):
   - `sun.jpg`
   - `mercury.jpg`
   - `venus_atmosphere.jpg`
   - `earth_daymap.jpg`, `earth_normal.jpg`, `earth_nightmap.jpg`, `earth_clouds.jpg`
   - `mars.jpg`
   - `jupiter.jpg`
   - `saturn.jpg`, `saturn_ring.png`
   - `uranus.jpg`
   - `neptune.jpg`
2. Place them into `app/src/main/assets/textures/`.
3. Rebuild the app — `TextureManager` will automatically pick up and bind the photographic assets.

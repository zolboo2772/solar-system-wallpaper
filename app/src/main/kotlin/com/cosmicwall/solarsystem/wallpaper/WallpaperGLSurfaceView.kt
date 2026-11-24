package com.cosmicwall.solarsystem.wallpaper

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.SurfaceHolder

class WallpaperGLSurfaceView(context: Context) : GLSurfaceView(context) {

    private var wallpaperSurfaceHolder: SurfaceHolder? = null

    fun setSurfaceHolder(holder: SurfaceHolder) {
        wallpaperSurfaceHolder = holder
    }

    override fun getHolder(): SurfaceHolder {
        return wallpaperSurfaceHolder ?: super.getHolder()
    }

    fun onWallpaperDestroy() {
        super.onDetachedFromWindow()
    }
}

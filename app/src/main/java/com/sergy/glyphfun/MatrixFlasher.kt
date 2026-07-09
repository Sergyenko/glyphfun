package com.sergy.glyphfun

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.VibratorManager

/**
 * Briefly shows today's tracker constellation on the matrix, then
 * releases the app-control channel again (the shared Glyph connection
 * itself stays up — see GlyphLink).
 */
object MatrixFlasher {

    private const val SHOW_MS = 4000L

    private val handler = Handler(Looper.getMainLooper())
    private var hideRunnable: Runnable? = null

    fun flash(context: Context) {
        val appContext = context.applicationContext
        hideRunnable?.let { handler.removeCallbacks(it) }
        GlyphLink.pushFrame(appContext, DayTracker.frame(appContext))
        val r = Runnable { GlyphLink.closeApp(appContext) }
        hideRunnable = r
        handler.postDelayed(r, SHOW_MS)
    }

    fun vibrate(context: Context) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 40, 80, 40), -1))
    }
}

package com.sergy.glyphfun

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Messenger
import com.nothing.ketchum.GlyphToy

/**
 * Glyph Toy: shows today's committed tracker pixels on the AOD.
 * Refreshes on the per-minute AOD tick, which also handles the
 * midnight reset.
 */
class DayTrackerToyService : Service() {

    private val toyHandler = Handler(Looper.getMainLooper()) { msg ->
        when (msg.what) {
            GlyphToy.MSG_GLYPH_TOY -> {
                val event = msg.data.getString(GlyphToy.MSG_GLYPH_TOY_DATA)
                if (event == GlyphToy.EVENT_AOD || event == GlyphToy.EVENT_CHANGE) {
                    draw()
                }
                true
            }
            else -> false
        }
    }
    private val messenger = Messenger(toyHandler)

    override fun onBind(intent: Intent?): IBinder {
        draw()
        return messenger.binder
    }

    private fun draw() {
        GlyphLink.pushFrame(this, DayTracker.frame(this), asApp = false)
    }
}

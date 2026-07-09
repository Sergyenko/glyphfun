package com.sergy.glyphfun

import android.content.ComponentName
import android.content.Context
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager

/**
 * Single shared connection to the Glyph service for the whole process.
 *
 * GlyphMatrixManager.getInstance() returns a process-wide singleton, so
 * one component calling unInit() kills the connection for everyone else.
 * All matrix access goes through here instead; the binding stays up for
 * the lifetime of the process. Actions issued before the service is
 * connected are queued and run on connect.
 */
object GlyphLink {

    private var gm: GlyphMatrixManager? = null
    private var ready = false
    private val pending = mutableListOf<(GlyphMatrixManager) -> Unit>()

    /** Notified with true on connect, false on disconnect. */
    var stateListener: ((Boolean) -> Unit)? = null

    @Synchronized
    fun run(context: Context, action: (GlyphMatrixManager) -> Unit) {
        val g = gm
        if (ready && g != null) {
            action(g)
            return
        }
        pending += action
        if (gm == null) {
            gm = GlyphMatrixManager.getInstance(context.applicationContext)
            gm?.init(object : GlyphMatrixManager.Callback {
                override fun onServiceConnected(name: ComponentName?) {
                    gm?.register(Glyph.DEVICE_25111p)
                    ready = true
                    stateListener?.invoke(true)
                    drain()
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    ready = false
                    gm = null
                    stateListener?.invoke(false)
                }
            })
        }
    }

    @Synchronized
    private fun drain() {
        val g = gm ?: return
        val actions = pending.toList()
        pending.clear()
        actions.forEach { it(g) }
    }

    /** asApp = true uses the app-control channel, false the toy channel. */
    fun pushFrame(context: Context, frame: IntArray, asApp: Boolean = true) =
        run(context) { g ->
            try {
                if (asApp) g.setAppMatrixFrame(frame) else g.setMatrixFrame(frame)
            } catch (e: Exception) {
                try {
                    g.setMatrixFrame(frame)
                } catch (_: Exception) {
                }
            }
        }

    fun closeApp(context: Context) = run(context) { g ->
        try {
            g.closeAppMatrix()
        } catch (_: Exception) {
        }
    }

    fun turnOff(context: Context) = run(context) { g ->
        try {
            g.closeAppMatrix()
        } catch (_: Exception) {
        }
        try {
            g.turnOff()
        } catch (_: Exception) {
        }
    }
}

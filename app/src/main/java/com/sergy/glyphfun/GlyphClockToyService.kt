package com.sergy.glyphfun

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Messenger
import com.nothing.ketchum.GlyphToy
import java.util.Calendar

/**
 * Glyph Toy: digital clock on the 13x13 matrix.
 * Hours on the top row, minutes below, drawn with a 3x5 pixel font.
 * On the Phone (4a) Pro this runs as an AOD toy — the system sends
 * EVENT_AOD roughly once a minute while it is the active toy.
 */
class GlyphClockToyService : Service() {

    private val size = GlyphGridView.SIZE

    private val toyHandler = Handler(Looper.getMainLooper()) { msg ->
        when (msg.what) {
            GlyphToy.MSG_GLYPH_TOY -> {
                val event = msg.data.getString(GlyphToy.MSG_GLYPH_TOY_DATA)
                if (event == GlyphToy.EVENT_AOD || event == GlyphToy.EVENT_CHANGE) {
                    drawClock()
                }
                true
            }
            else -> false
        }
    }
    private val messenger = Messenger(toyHandler)

    override fun onBind(intent: Intent?): IBinder {
        drawClock()
        return messenger.binder
    }

    private fun drawClock() {
        val now = Calendar.getInstance()
        val frame = IntArray(size * size)
        drawNumber(frame, now.get(Calendar.HOUR_OF_DAY), y = 1)
        drawNumber(frame, now.get(Calendar.MINUTE), y = 7)
        GlyphLink.pushFrame(this, frame, asApp = false)
    }

    /** Draws a two-digit number centered horizontally: 3px digit, 1px gap, 3px digit. */
    private fun drawNumber(frame: IntArray, value: Int, y: Int) {
        drawDigit(frame, value / 10, x = 3, y = y)
        drawDigit(frame, value % 10, x = 7, y = y)
    }

    private fun drawDigit(frame: IntArray, digit: Int, x: Int, y: Int) {
        val glyph = FONT[digit]
        for (row in 0 until 5) {
            for (col in 0 until 3) {
                if (glyph[row] shr (2 - col) and 1 == 1) {
                    val px = x + col
                    val py = y + row
                    if (px in 0 until size && py in 0 until size) {
                        frame[py * size + px] = 255
                    }
                }
            }
        }
    }

    companion object {
        /** 3x5 font, one entry per digit, 3-bit rows top to bottom. */
        private val FONT = arrayOf(
            intArrayOf(0b111, 0b101, 0b101, 0b101, 0b111), // 0
            intArrayOf(0b010, 0b110, 0b010, 0b010, 0b111), // 1
            intArrayOf(0b111, 0b001, 0b111, 0b100, 0b111), // 2
            intArrayOf(0b111, 0b001, 0b111, 0b001, 0b111), // 3
            intArrayOf(0b101, 0b101, 0b111, 0b001, 0b001), // 4
            intArrayOf(0b111, 0b100, 0b111, 0b001, 0b111), // 5
            intArrayOf(0b111, 0b100, 0b111, 0b101, 0b111), // 6
            intArrayOf(0b111, 0b001, 0b010, 0b010, 0b010), // 7
            intArrayOf(0b111, 0b101, 0b111, 0b101, 0b111), // 8
            intArrayOf(0b111, 0b101, 0b111, 0b001, 0b111), // 9
        )
    }
}

package com.sergy.glyphfun

import android.content.Context
import java.time.LocalDate
import kotlin.random.Random

/**
 * Stores the pixels committed today, in commit order.
 * Resets automatically when the date changes.
 */
object DayTracker {

    private const val PREFS = "day_tracker"
    private const val KEY_DATE = "date"
    private const val KEY_PIXELS = "pixels"
    private const val TOTAL = GlyphGridView.SIZE * GlyphGridView.SIZE

    /** Today's committed pixel indices, oldest first. */
    fun pixels(context: Context): List<Int> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        if (prefs.getString(KEY_DATE, "") != today) {
            prefs.edit().putString(KEY_DATE, today).putString(KEY_PIXELS, "").apply()
            return emptyList()
        }
        val csv = prefs.getString(KEY_PIXELS, "") ?: ""
        return if (csv.isEmpty()) emptyList() else csv.split(',').map { it.toInt() }
    }

    /**
     * Commits one random not-yet-lit pixel.
     * Returns the new pixel index, or null if the whole matrix is already lit.
     */
    fun commit(context: Context): Int? {
        val current = pixels(context)
        val free = (0 until TOTAL).filterNot { it in current.toSet() }
        if (free.isEmpty()) return null
        val pick = free[Random.nextInt(free.size)]
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_PIXELS, (current + pick).joinToString(","))
            .apply()
        return pick
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_PIXELS, "").apply()
    }

    /** Renders today's pixels: history dim, the most recent one at full brightness. */
    fun frame(context: Context): IntArray {
        val out = IntArray(TOTAL)
        val list = pixels(context)
        list.forEachIndexed { i, p ->
            out[p] = if (i == list.lastIndex) 255 else 110
        }
        return out
    }
}

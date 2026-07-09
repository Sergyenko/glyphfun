package com.sergy.glyphfun

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Log of completed pomodoros: when, what for, and how it went.
 * Stored as JSON in SharedPreferences, capped to the last 500 entries.
 */
object PomodoroLog {

    data class Entry(
        val ts: Long,
        val reason: String,
        val rating: String?,
        val completed: Boolean = true,
        val note: String = "",
        val durationMs: Long = 0,
    )

    const val RATING_GOOD = "good"
    const val RATING_BAD = "bad"

    private const val PREFS = "pomodoro_log"
    private const val KEY = "entries"
    private const val MAX_ENTRIES = 500

    fun entries(context: Context): List<Entry> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val completed = o.optBoolean("completed", true)
            Entry(o.getLong("ts"), o.optString("reason"),
                o.optString("rating").ifEmpty { null },
                completed,
                o.optString("note"),
                o.optLong("dur", if (completed) 25 * 60_000L else 0))
        }
    }

    fun todayEntries(context: Context): List<Entry> {
        val today = LocalDate.now()
        return entries(context).filter {
            Instant.ofEpochMilli(it.ts).atZone(ZoneId.systemDefault()).toLocalDate() == today
        }
    }

    fun add(context: Context, reason: String, durationMs: Long) {
        save(context, entries(context) +
            Entry(System.currentTimeMillis(), reason, null, durationMs = durationMs))
    }

    fun addInterrupted(context: Context, reason: String, note: String, durationMs: Long) {
        save(context, entries(context) +
            Entry(System.currentTimeMillis(), reason, null,
                completed = false, note = note, durationMs = durationMs))
    }

    fun delete(context: Context, ts: Long) {
        save(context, entries(context).filterNot { it.ts == ts })
    }

    fun rate(context: Context, ts: Long, rating: String) {
        save(context, entries(context).map {
            if (it.ts == ts) it.copy(rating = rating) else it
        })
    }

    private fun save(context: Context, list: List<Entry>) {
        val arr = JSONArray()
        list.takeLast(MAX_ENTRIES).forEach { e ->
            arr.put(JSONObject().put("ts", e.ts).put("reason", e.reason).apply {
                e.rating?.let { put("rating", it) }
                if (!e.completed) put("completed", false)
                if (e.note.isNotEmpty()) put("note", e.note)
                put("dur", e.durationMs)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, arr.toString()).apply()
    }
}

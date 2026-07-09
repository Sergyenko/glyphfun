package com.sergy.glyphfun

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.VibratorManager
import android.service.quicksettings.TileService
import kotlin.math.ceil
import kotlin.random.Random

/**
 * Pomodoro timer as a foreground service, visualized on the Glyph
 * Matrix as a draining hourglass: all 169 pixels lit at the start of a
 * phase, emptying from the top as time passes. Focus is bright, break
 * is dim. Phase changes buzz and blink the matrix, then roll over
 * automatically until stopped.
 */
class PomodoroService : Service() {

    enum class Phase(val label: String, val durationMs: Long, val brightness: Int) {
        FOCUS("Focus", 25 * 60_000L, 255),
        BREAK("Break", 5 * 60_000L, 110)
    }

    companion object {
        const val ACTION_START = "com.sergy.glyphfun.POMODORO_START"
        const val ACTION_STOP = "com.sergy.glyphfun.POMODORO_STOP"
        const val EXTRA_REASON = "com.sergy.glyphfun.EXTRA_REASON"

        /** What the current session is for; shown and logged on completion. */
        @Volatile
        var currentReason = ""
        private const val CHANNEL = "pomodoro"
        private const val NOTIF_ID = 1

        // Static state so the QS tile and activity can render without binding.
        @Volatile
        var running = false
        @Volatile
        var phase = Phase.FOCUS
        @Volatile
        private var endsAt = 0L

        /** Last frame pushed to the matrix, for mirroring in the app UI. */
        @Volatile
        var currentFrame: IntArray? = null
            private set

        fun remainingMs(): Long =
            if (!running) 0L else (endsAt - SystemClock.elapsedRealtime()).coerceAtLeast(0L)

        fun remainingMinutes(): Long = (remainingMs() + 59_999) / 60_000

        /** Time spent in the current phase so far. */
        fun elapsedMs(): Long = if (!running) 0 else phase.durationMs - remainingMs()

        /** Elapsed fraction of the current phase, 0..1. */
        fun progressFraction(): Float =
            if (!running) 0f else 1f - remainingMs().toFloat() / phase.durationMs

        fun remainingText(): String {
            val s = remainingMs() / 1000
            return "%d:%02d".format(s / 60, s % 60)
        }
    }

    private val size = GlyphGridView.SIZE
    private val total = size * size
    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastNotifiedMinute = -1L
    private var blinkTicksLeft = 0

    // The "reaper": a half-bright pixel wandering across the lit field;
    // whatever it stands on when a removal is due goes dark.
    private var alive = BooleanArray(total) { true }
    private var aliveCount = total
    private var reaper = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentReason = intent.getStringExtra(EXTRA_REASON)?.trim().orEmpty()
                start()
            }
            ACTION_STOP -> stop()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        wakeLock?.let { if (it.isHeld) it.release() }
        running = false
        super.onDestroy()
    }

    private fun start() {
        createChannel()
        startForeground(NOTIF_ID, buildNotification())
        phase = Phase.FOCUS
        endsAt = SystemClock.elapsedRealtime() + phase.durationMs
        running = true
        lastNotifiedMinute = -1
        resetField()
        acquireWakeLock(phase.durationMs)
        handler.removeCallbacksAndMessages(null)
        handler.post(tick)
        notifyTile()
    }

    private fun stop() {
        running = false
        currentFrame = null
        handler.removeCallbacksAndMessages(null)
        wakeLock?.let { if (it.isHeld) it.release() }
        GlyphLink.closeApp(this)
        notifyTile()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            if (blinkTicksLeft > 0) {
                blinkTick()
            } else if (remainingMs() <= 0) {
                onPhaseEnd()
            } else {
                drawProgress()
                updateNotificationAndTilePerMinute()
            }
            handler.postDelayed(this, if (blinkTicksLeft > 0) 250 else 1000)
        }
    }

    private fun onPhaseEnd() {
        if (phase == Phase.FOCUS) {
            // Focus done: log it and roll into the break.
            PomodoroLog.add(this, currentReason, phase.durationMs)
            beep()
            vibrate(longArrayOf(0, 350, 150, 350, 150, 350))
            phase = Phase.BREAK
            endsAt = SystemClock.elapsedRealtime() + phase.durationMs
            lastNotifiedMinute = -1
            resetField()
            acquireWakeLock(phase.durationMs)
            blinkTicksLeft = 6
        } else {
            // Break done: one full cycle — stop instead of looping.
            vibrate(longArrayOf(0, 120, 80, 120))
            stop()
        }
    }

    /** Alternates full/empty frames a few times to mark a phase change. */
    private fun blinkTick() {
        val on = blinkTicksLeft % 2 == 0
        val frame = IntArray(total) { if (on) phase.brightness else 0 }
        currentFrame = frame
        GlyphLink.pushFrame(this, frame)
        blinkTicksLeft--
    }

    private fun resetField() {
        alive.fill(true)
        aliveCount = total
        reaper = Random.nextInt(total)
    }

    /**
     * The reaper wanders one step per second across still-lit pixels at
     * half brightness. When the elapsed time calls for another removal,
     * the pixel it currently stands on goes dark for the rest of the
     * phase, and the walk continues over what remains.
     */
    private fun drawProgress() {
        val fraction = remainingMs().toDouble() / phase.durationMs
        val target = ceil(fraction * total).toInt().coerceIn(0, total)
        while (aliveCount > target) {
            alive[reaper] = false
            aliveCount--
            moveReaper()
        }
        moveReaper()
        val frame = IntArray(total)
        for (i in 0 until total) if (alive[i]) frame[i] = phase.brightness
        if (aliveCount > 0) frame[reaper] = phase.brightness / 2
        currentFrame = frame
        GlyphLink.pushFrame(this, frame)
    }

    /** Steps to a random lit neighbour, or teleports if boxed in. */
    private fun moveReaper() {
        if (aliveCount == 0) return
        val x = reaper % size
        val y = reaper / size
        val neighbors = mutableListOf<Int>()
        for (dy in -1..1) {
            for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until size && ny in 0 until size && alive[ny * size + nx]) {
                    neighbors += ny * size + nx
                }
            }
        }
        reaper = if (neighbors.isNotEmpty()) neighbors.random()
                 else (0 until total).filter { alive[it] }.random()
    }

    private var tickCount = 0

    private fun updateNotificationAndTilePerMinute() {
        if (tickCount++ % 10 == 0) {
            getSystemService(NotificationManager::class.java)
                .notify(NOTIF_ID, buildNotification())
        }
        val minute = remainingMinutes()
        if (minute != lastNotifiedMinute) {
            lastNotifiedMinute = minute
            notifyTile()
        }
    }

    private fun acquireWakeLock(durationMs: Long) {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "glyphfun:pomodoro")
            .apply { acquire(durationMs + 60_000) }
    }

    private fun beep() {
        try {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
            handler.postDelayed({ tone.release() }, 700)
        } catch (_: Exception) {
        }
    }

    private fun vibrate(pattern: LongArray) {
        val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    private fun notifyTile() {
        TileService.requestListeningState(
            this, ComponentName(this, PomodoroTileService::class.java))
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Pomodoro", NotificationManager.IMPORTANCE_LOW))
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, PomodoroService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_pomodoro)
            .setContentTitle("Pomodoro — ${phase.label}")
            .setContentText(if (running) "${remainingText()} left" else "Starting…")
            .setProgress(1000, (progressFraction() * 1000).toInt(), false)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Stop", stopIntent).build())
            .build()
    }
}

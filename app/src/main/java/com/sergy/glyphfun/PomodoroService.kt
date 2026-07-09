package com.sergy.glyphfun

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.VibratorManager
import android.service.quicksettings.TileService
import kotlin.math.ceil

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
        BREAK("Break", 5 * 60_000L, 110);

        fun next() = if (this == FOCUS) BREAK else FOCUS
    }

    companion object {
        const val ACTION_START = "com.sergy.glyphfun.POMODORO_START"
        const val ACTION_STOP = "com.sergy.glyphfun.POMODORO_STOP"
        private const val CHANNEL = "pomodoro"
        private const val NOTIF_ID = 1

        // Static state so the QS tile and activity can render without binding.
        @Volatile
        var running = false
        @Volatile
        var phase = Phase.FOCUS
        @Volatile
        private var endsAt = 0L

        fun remainingMs(): Long =
            if (!running) 0L else (endsAt - SystemClock.elapsedRealtime()).coerceAtLeast(0L)

        fun remainingMinutes(): Long = (remainingMs() + 59_999) / 60_000

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
    private var lastLit = -1
    private var lastNotifiedMinute = -1L
    private var blinkTicksLeft = 0
    private var drainOrder = (0 until total).shuffled().toIntArray()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
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
        lastLit = -1
        lastNotifiedMinute = -1
        drainOrder = (0 until total).shuffled().toIntArray()
        acquireWakeLock(phase.durationMs)
        handler.removeCallbacksAndMessages(null)
        handler.post(tick)
        notifyTile()
    }

    private fun stop() {
        running = false
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
        vibrate(if (phase == Phase.FOCUS) longArrayOf(0, 350, 150, 350, 150, 350)
                else longArrayOf(0, 120, 80, 120))
        phase = phase.next()
        endsAt = SystemClock.elapsedRealtime() + phase.durationMs
        lastLit = -1
        lastNotifiedMinute = -1
        drainOrder = (0 until total).shuffled().toIntArray()
        acquireWakeLock(phase.durationMs)
        blinkTicksLeft = 6
    }

    /** Alternates full/empty frames a few times to mark a phase change. */
    private fun blinkTick() {
        val on = blinkTicksLeft % 2 == 0
        val frame = IntArray(total) { if (on) phase.brightness else 0 }
        GlyphLink.pushFrame(this, frame)
        blinkTicksLeft--
    }

    /**
     * Pixels wink out one by one at random positions as time passes.
     * The removal order is shuffled once per phase so already-dark
     * pixels stay dark.
     */
    private fun drawProgress() {
        val fraction = remainingMs().toDouble() / phase.durationMs
        val lit = ceil(fraction * total).toInt().coerceIn(0, total)
        if (lit == lastLit) return
        lastLit = lit
        val frame = IntArray(total) { phase.brightness }
        for (i in 0 until total - lit) frame[drainOrder[i]] = 0
        GlyphLink.pushFrame(this, frame)
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

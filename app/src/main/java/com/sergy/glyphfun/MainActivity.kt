package com.sergy.glyphfun

import android.app.Activity
import android.app.AlertDialog
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : Activity() {

    private val size = GlyphGridView.SIZE
    private lateinit var grid: GlyphGridView
    private lateinit var status: TextView
    private lateinit var trackerLabel: TextView
    private lateinit var pomodoroHeader: TextView
    private lateinit var pomodoroBar: ProgressBar

    private val handler = Handler(Looper.getMainLooper())
    private var animator: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()

        GlyphLink.stateListener = { connected ->
            runOnUiThread {
                status.text = if (connected) "Connected to Glyph Matrix"
                else "Glyph service disconnected"
            }
        }
        GlyphLink.run(this) { pushGrid() }

        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onDestroy() {
        stopAnimation()
        GlyphLink.stateListener = null
        GlyphLink.closeApp(this)
        super.onDestroy()
    }

    private fun buildUi() {
        grid = GlyphGridView(this).apply {
            onGridChanged = {
                stopAnimation()
                pushGrid()
            }
        }
        status = TextView(this).apply {
            text = "Connecting to Glyph service…"
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 24)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(32, 64, 32, 32)
        }
        root.addView(TextView(this).apply {
            text = "GlyphFun — draw on the matrix"
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        })
        root.addView(grid)
        root.addView(status)
        root.addView(buttonRow("Clear" to { stopAnimation(); grid.clear() },
            "Rain" to { startRain() },
            "Spiral" to { startSpiral() }))
        root.addView(buttonRow("Life" to { startLife() },
            "Random" to { startSparkle() },
            "Off" to { stopAnimation(); grid.clear(); turnOff() },
            "Toys" to { openToyManager() }))
        root.addView(TextView(this).apply {
            text = "Presets"
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 8)
        })
        val presetRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        PRESETS.forEach { preset ->
            presetRow.addView(Button(this).apply {
                text = preset.name
                setOnClickListener { startPreset(preset) }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        root.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(presetRow)
        })
        trackerLabel = TextView(this).apply {
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 8)
        }
        root.addView(trackerLabel)
        root.addView(buttonRow(
            "Show" to { showTrackerDay() },
            "Commit ✦" to { commitPixel() },
            "Add tile" to { requestTile() },
            "Reset" to { confirmReset() }))
        pomodoroHeader = TextView(this).apply {
            text = "Pomodoro"
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 8)
        }
        root.addView(pomodoroHeader)
        pomodoroBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            visibility = View.GONE
        }
        root.addView(pomodoroBar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        root.addView(buttonRow(
            "▶ Focus" to { pomodoro(PomodoroService.ACTION_START) },
            "■ Stop" to { pomodoro(PomodoroService.ACTION_STOP) },
            "Add tile" to { requestPomodoroTile() }))
        setContentView(root)
        refreshTrackerLabel()
    }

    private fun buttonRow(vararg items: Pair<String, () -> Unit>): LinearLayout {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        items.forEach { (label, action) ->
            row.addView(Button(this).apply {
                text = label
                setOnClickListener { action() }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        return row
    }

    // --- Matrix output -------------------------------------------------

    private fun pushGrid() = pushFrame(grid.pixels)

    private fun pushFrame(frame: IntArray) = GlyphLink.pushFrame(this, frame)

    private fun turnOff() = GlyphLink.turnOff(this)

    private fun openToyManager() {
        try {
            startActivity(Intent().setComponent(ComponentName(
                "com.nothing.thirdparty",
                "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity")))
        } catch (e: Exception) {
            status.text = "Toy manager not available on this system version"
        }
    }

    // --- Day tracker -----------------------------------------------------

    private fun refreshTrackerLabel() {
        trackerLabel.text = "Day tracker — ${DayTracker.pixels(this).size} ✦ today"
    }

    private fun showTrackerDay() {
        stopAnimation()
        val frame = DayTracker.frame(this)
        grid.setPixels(frame)
        pushFrame(frame)
        refreshTrackerLabel()
    }

    private fun commitPixel() {
        DayTracker.commit(this)
        MatrixFlasher.vibrate(this)
        showTrackerDay()
    }

    private fun confirmReset() {
        val count = DayTracker.pixels(this).size
        if (count == 0) {
            status.text = "Nothing to reset — no pixels today"
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Reset day tracker?")
            .setMessage("This deletes all $count pixels committed today. There is no undo.")
            .setPositiveButton("Reset") { _, _ ->
                DayTracker.clear(this)
                showTrackerDay()
                status.text = "Day tracker reset"
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestTile() =
        requestTile(CommitTileService::class.java, R.string.tile_label, R.drawable.ic_tile)

    private fun requestPomodoroTile() =
        requestTile(PomodoroTileService::class.java, R.string.pomodoro_tile_label, R.drawable.ic_pomodoro)

    private fun requestTile(service: Class<*>, labelRes: Int, iconRes: Int) {
        val sbm = getSystemService(StatusBarManager::class.java)
        sbm.requestAddTileService(
            ComponentName(this, service),
            getString(labelRes),
            Icon.createWithResource(this, iconRes),
            mainExecutor
        ) { result ->
            runOnUiThread {
                status.text = when (result) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> "Tile added — swipe down to use it"
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> "Tile is already in Quick Settings"
                    else -> "Tile not added"
                }
            }
        }
    }

    // --- Pomodoro --------------------------------------------------------

    private val pomodoroUpdater = object : Runnable {
        override fun run() {
            if (PomodoroService.running) {
                pomodoroBar.visibility = View.VISIBLE
                pomodoroBar.progress = (PomodoroService.progressFraction() * 1000).toInt()
                pomodoroHeader.text =
                    "Pomodoro — ${PomodoroService.phase.label} · ${PomodoroService.remainingText()} left"
            } else {
                pomodoroBar.visibility = View.GONE
                pomodoroHeader.text = "Pomodoro"
            }
            handler.postDelayed(this, 500)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(pomodoroUpdater)
        refreshTrackerLabel()
    }

    override fun onPause() {
        handler.removeCallbacks(pomodoroUpdater)
        super.onPause()
    }

    private fun pomodoro(action: String) {
        if (action == PomodoroService.ACTION_STOP && !PomodoroService.running) {
            status.text = "Pomodoro is not running"
            return
        }
        stopAnimation()
        startForegroundService(Intent(this, PomodoroService::class.java).setAction(action))
        status.text = if (action == PomodoroService.ACTION_START)
            "Pomodoro started — 25 min focus" else "Pomodoro stopped"
    }

    // --- Animations ----------------------------------------------------

    private fun startPreset(preset: Preset) {
        startAnimation(preset.frameMs) { tick, frame ->
            preset.frames[tick % preset.frames.size].copyInto(frame)
        }
    }

    private fun stopAnimation() {
        animator?.let { handler.removeCallbacks(it) }
        animator = null
    }

    private fun startAnimation(frameMs: Long, step: (tick: Int, frame: IntArray) -> Unit) {
        stopAnimation()
        var tick = 0
        val frame = IntArray(size * size)
        val r = object : Runnable {
            override fun run() {
                step(tick++, frame)
                grid.setPixels(frame)
                pushFrame(frame)
                handler.postDelayed(this, frameMs)
            }
        }
        animator = r
        handler.post(r)
    }

    private fun startRain() {
        val drops = IntArray(size) { -Random.nextInt(size) }
        startAnimation(90) { _, frame ->
            frame.fill(0)
            for (x in 0 until size) {
                val y = drops[x]
                if (y in 0 until size) frame[y * size + x] = 255
                if (y - 1 in 0 until size) frame[(y - 1) * size + x] = 90
                drops[x] = if (y > size + 2) -Random.nextInt(6) else y + 1
            }
        }
    }

    private fun startSpiral() {
        val c = (size - 1) / 2.0
        startAnimation(60) { tick, frame ->
            for (i in frame.indices) frame[i] = (frame[i] * 0.72).toInt()
            val angle = tick * 0.35
            for (r in 0..6) {
                val x = (c + r * cos(angle + r * 0.5)).toInt()
                val y = (c + r * sin(angle + r * 0.5)).toInt()
                if (x in 0 until size && y in 0 until size) frame[y * size + x] = 255
            }
        }
    }

    /**
     * Conway's Game of Life on the matrix. Seeds from the drawn grid
     * (random soup if empty), wraps around the edges, and reseeds when
     * the colony dies or settles into a short cycle. Dying cells leave
     * a fading ghost trail.
     */
    private fun startLife() {
        var cells = grid.pixels.map { if (it > 0) 1 else 0 }.toIntArray()
        if (cells.none { it == 1 }) cells = randomSoup()
        val recentStates = ArrayDeque<Int>()
        startAnimation(200) { _, frame ->
            for (i in frame.indices) {
                frame[i] = if (cells[i] == 1) 255 else (frame[i] * 0.45).toInt()
            }
            cells = lifeStep(cells)
            val hash = cells.contentHashCode()
            if (cells.none { it == 1 } || hash in recentStates) {
                cells = randomSoup()
                recentStates.clear()
            } else {
                recentStates.addLast(hash)
                if (recentStates.size > 6) recentStates.removeFirst()
            }
        }
    }

    private fun lifeStep(cells: IntArray): IntArray {
        val next = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                var neighbors = 0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        neighbors += cells[((y + dy + size) % size) * size + (x + dx + size) % size]
                    }
                }
                val alive = cells[y * size + x] == 1
                next[y * size + x] = if (neighbors == 3 || (alive && neighbors == 2)) 1 else 0
            }
        }
        return next
    }

    private fun randomSoup(): IntArray =
        IntArray(size * size) { if (Random.nextFloat() < 0.3f) 1 else 0 }

    private fun startSparkle() {
        startAnimation(80) { _, frame ->
            for (i in frame.indices) frame[i] = (frame[i] * 0.8).toInt()
            repeat(4) {
                frame[Random.nextInt(frame.size)] = 255
            }
        }
    }
}

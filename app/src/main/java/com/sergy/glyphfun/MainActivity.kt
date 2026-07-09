package com.sergy.glyphfun

import android.app.Activity
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class MainActivity : Activity() {

    private val size = GlyphGridView.SIZE
    private lateinit var grid: GlyphGridView
    private lateinit var status: TextView
    private lateinit var pomodoroHeader: TextView
    private lateinit var pomodoroBar: ProgressBar
    private lateinit var customText: EditText
    private lateinit var reasonInput: EditText
    private lateinit var glyphTab: LinearLayout
    private lateinit var statsTab: ScrollView
    private lateinit var statsContent: LinearLayout
    private lateinit var tabGlyph: TextView
    private lateinit var tabStats: TextView
    private var statsVisible = false
    private var lastStatsCount = -1

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
            // Edge-to-edge (targetSdk 35): keep content clear of the
            // status bar / camera cutout and the gesture bar.
            setOnApplyWindowInsetsListener { v, insets ->
                val bars = insets.getInsets(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                v.setPadding(32, bars.top + 32, 32, bars.bottom + 32)
                insets
            }
        }
        // Tab bar: Glyph playground | Pomodoro stats
        tabGlyph = tabLabel("Glyph") { selectTab(stats = false) }
        tabStats = tabLabel("🍅 Stats") { selectTab(stats = true) }
        val tabRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tabRow.addView(tabGlyph, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        tabRow.addView(tabStats, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(tabRow)

        glyphTab = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        statsContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 16)
        }
        statsTab = ScrollView(this).apply {
            visibility = View.GONE
            addView(statsContent)
        }

        glyphTab.addView(grid)
        glyphTab.addView(status)
        val animations = listOf<Pair<String, () -> Unit>>("Life" to { startLife() }) +
            PRESETS.map { preset -> preset.name to { startPreset(preset) } }
        animations.chunked(4).forEach { chunk ->
            glyphTab.addView(buttonRow(*chunk.toTypedArray()))
        }
        customText = EditText(this).apply {
            hint = "Custom running text…"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, _, _ -> runCustomText(shimmer = false); true }
        }
        val textRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        textRow.addView(customText,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        textRow.addView(Button(this).apply {
            text = "Run"
            setOnClickListener { runCustomText(shimmer = false) }
        })
        textRow.addView(Button(this).apply {
            text = "✨"
            setOnClickListener { runCustomText(shimmer = true) }
        })
        glyphTab.addView(textRow)
        pomodoroHeader = TextView(this).apply {
            text = "🍅 Pomodoro"
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 8)
        }
        glyphTab.addView(pomodoroHeader)
        pomodoroBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            visibility = View.GONE
        }
        glyphTab.addView(pomodoroBar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        reasonInput = EditText(this).apply {
            hint = "What are you working on?"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            isSingleLine = true
        }
        glyphTab.addView(reasonInput)
        glyphTab.addView(buttonRow(
            "▶ Focus" to { pomodoro(PomodoroService.ACTION_START) },
            "■ Stop" to { pomodoro(PomodoroService.ACTION_STOP) },
            "Add tile" to { requestPomodoroTile() }))

        // Spacer pushes the control icons to the very bottom.
        glyphTab.addView(View(this),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        val density = resources.displayMetrics.density
        val buttonSize = (56 * density).toInt()
        val buttonMargin = (20 * density).toInt()
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        controls.addView(roundButton("🧹") { stopAnimation(); grid.clear() },
            LinearLayout.LayoutParams(buttonSize, buttonSize).apply {
                marginStart = buttonMargin
                marginEnd = buttonMargin
            })
        glyphTab.addView(controls, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT))

        root.addView(glyphTab, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(statsTab, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        selectTab(stats = false)
    }

    private fun tabLabel(label: String, onClick: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 24)
            setOnClickListener { onClick() }
        }

    private fun selectTab(stats: Boolean) {
        statsVisible = stats
        statsTab.visibility = if (stats) View.VISIBLE else View.GONE
        glyphTab.visibility = if (stats) View.GONE else View.VISIBLE
        tabStats.setTextColor(if (stats) Color.WHITE else Color.GRAY)
        tabGlyph.setTextColor(if (stats) Color.GRAY else Color.WHITE)
        if (stats) rebuildStats()
    }

    /** Today's completed pomodoros, newest first, with 👍/👎 rating. */
    private fun rebuildStats() {
        statsContent.removeAllViews()
        val entries = PomodoroLog.todayEntries(this).sortedByDescending { it.ts }
        lastStatsCount = entries.size
        val good = entries.count { it.rating == PomodoroLog.RATING_GOOD }
        val bad = entries.count { it.rating == PomodoroLog.RATING_BAD }
        statsContent.addView(TextView(this).apply {
            text = "Today — ${entries.size} 🍅    👍 $good · 👎 $bad"
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 24)
        })
        if (entries.isEmpty()) {
            statsContent.addView(TextView(this).apply {
                text = "No pomodoros completed today yet.\nStart one from the Glyph tab or the QS tile."
                setTextColor(Color.GRAY)
                gravity = Gravity.CENTER
                setPadding(0, 48, 0, 0)
            })
            return
        }
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        entries.forEach { entry ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 8, 0, 8)
            }
            row.addView(TextView(this).apply {
                text = "${fmt.format(Date(entry.ts))}   ${entry.reason.ifEmpty { "(no goal)" }}"
                setTextColor(Color.LTGRAY)
                textSize = 15f
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            listOf(PomodoroLog.RATING_GOOD to "👍", PomodoroLog.RATING_BAD to "👎")
                .forEach { (value, icon) ->
                    row.addView(TextView(this).apply {
                        text = icon
                        textSize = 20f
                        alpha = if (entry.rating == value) 1f else 0.3f
                        setPadding(28, 8, 28, 8)
                        setOnClickListener {
                            PomodoroLog.rate(this@MainActivity, entry.ts, value)
                            rebuildStats()
                        }
                    })
                }
            statsContent.addView(row)
        }
    }

    private fun roundButton(symbol: String, action: () -> Unit): TextView =
        TextView(this).apply {
            text = symbol
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(45, 45, 45))
            }
            setOnClickListener { action() }
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
                    "🍅 ${PomodoroService.phase.label} · ${PomodoroService.remainingText()} left"
                // Mirror the dissolving pixel field onto the on-screen grid,
                // unless a local animation is using it.
                if (animator == null) {
                    PomodoroService.currentFrame?.let { grid.setPixels(it) }
                }
            } else {
                pomodoroBar.visibility = View.GONE
                pomodoroHeader.text = "🍅 Pomodoro"
            }
            // Live-refresh the stats list when a pomodoro completes.
            if (statsVisible && PomodoroLog.todayEntries(this@MainActivity).size != lastStatsCount) {
                rebuildStats()
            }
            handler.postDelayed(this, 500)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(pomodoroUpdater)
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
        startForegroundService(Intent(this, PomodoroService::class.java)
            .setAction(action)
            .putExtra(PomodoroService.EXTRA_REASON, reasonInput.text.toString()))
        status.text = if (action == PomodoroService.ACTION_START)
            "Pomodoro started — 25 min focus" else "Pomodoro stopped"
    }

    // --- Animations ----------------------------------------------------

    private fun startPreset(preset: Preset) {
        startAnimation(preset.frameMs) { tick, frame ->
            preset.frames[tick % preset.frames.size].copyInto(frame)
        }
    }

    private fun runCustomText(shimmer: Boolean) {
        val text = customText.text.toString().trim()
        if (text.isEmpty()) {
            status.text = "Type something first"
            return
        }
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(customText.windowToken, 0)
        val frames = if (shimmer) gradientMarqueeFrames(text) else marqueeFrames(text)
        startAnimation(110) { tick, frame ->
            frames[tick % frames.size].copyInto(frame)
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

}

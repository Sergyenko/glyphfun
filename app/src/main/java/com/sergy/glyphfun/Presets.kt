package com.sergy.glyphfun

/**
 * Animated presets for the Glyph Matrix, composed from string-art
 * sprites ('X' bright, 'o' dim, '.' transparent) blitted at offsets.
 */
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class Preset(val name: String, val frames: List<IntArray>, val frameMs: Long)

private const val SIZE = GlyphGridView.SIZE
private const val TOTAL = SIZE * SIZE

private fun rows(art: String): List<String> = art.trimIndent().lines()

private fun blit(frame: IntArray, sprite: List<String>, x: Int, y: Int) {
    sprite.forEachIndexed { dy, row ->
        row.forEachIndexed { dx, ch ->
            val v = when (ch) {
                'X' -> 255
                'o' -> 110
                else -> 0
            }
            if (v > 0) {
                val px = x + dx
                val py = y + dy
                if (px in 0 until SIZE && py in 0 until SIZE) frame[py * SIZE + px] = v
            }
        }
    }
}

// --- "67" ---------------------------------------------------------------

private val SIX = rows("""
    .XXX.
    X....
    X....
    XXXX.
    X...X
    X...X
    .XXX.
""")

private val SEVEN = rows("""
    XXXXX
    ....X
    ...X.
    ..X..
    ..X..
    .X...
    .X...
""")

/** The digits bob in opposite directions, meme-style. */
private fun sixSevenFrames(): List<IntArray> {
    fun pose(dy6: Int, dy7: Int) = IntArray(TOTAL).also {
        blit(it, SIX, 1, 3 + dy6)
        blit(it, SEVEN, 7, 3 + dy7)
    }
    return listOf(pose(-1, 1), pose(0, 0), pose(1, -1), pose(0, 0))
}

// --- "FUCK YOU" + middle finger ------------------------------------------

private val FONT = mapOf(
    'A' to rows("""
        .XXX.
        X...X
        X...X
        XXXXX
        X...X
        X...X
        X...X
    """),
    'B' to rows("""
        XXXX.
        X...X
        X...X
        XXXX.
        X...X
        X...X
        XXXX.
    """),
    'C' to rows("""
        .XXXX
        X....
        X....
        X....
        X....
        X....
        .XXXX
    """),
    'D' to rows("""
        XXXX.
        X...X
        X...X
        X...X
        X...X
        X...X
        XXXX.
    """),
    'E' to rows("""
        XXXXX
        X....
        X....
        XXXX.
        X....
        X....
        XXXXX
    """),
    'F' to rows("""
        XXXXX
        X....
        X....
        XXXX.
        X....
        X....
        X....
    """),
    'G' to rows("""
        .XXXX
        X....
        X....
        X.XXX
        X...X
        X...X
        .XXX.
    """),
    'H' to rows("""
        X...X
        X...X
        X...X
        XXXXX
        X...X
        X...X
        X...X
    """),
    'I' to rows("""
        XXXXX
        ..X..
        ..X..
        ..X..
        ..X..
        ..X..
        XXXXX
    """),
    'J' to rows("""
        ..XXX
        ...X.
        ...X.
        ...X.
        ...X.
        X..X.
        .XX..
    """),
    'K' to rows("""
        X...X
        X..X.
        X.X..
        XX...
        X.X..
        X..X.
        X...X
    """),
    'L' to rows("""
        X....
        X....
        X....
        X....
        X....
        X....
        XXXXX
    """),
    'M' to rows("""
        X...X
        XX.XX
        X.X.X
        X.X.X
        X...X
        X...X
        X...X
    """),
    'N' to rows("""
        X...X
        XX..X
        X.X.X
        X..XX
        X...X
        X...X
        X...X
    """),
    'O' to rows("""
        .XXX.
        X...X
        X...X
        X...X
        X...X
        X...X
        .XXX.
    """),
    'P' to rows("""
        XXXX.
        X...X
        X...X
        XXXX.
        X....
        X....
        X....
    """),
    'Q' to rows("""
        .XXX.
        X...X
        X...X
        X...X
        X.X.X
        X..X.
        .XX.X
    """),
    'R' to rows("""
        XXXX.
        X...X
        X...X
        XXXX.
        X.X..
        X..X.
        X...X
    """),
    'S' to rows("""
        .XXXX
        X....
        X....
        .XXX.
        ....X
        ....X
        XXXX.
    """),
    'T' to rows("""
        XXXXX
        ..X..
        ..X..
        ..X..
        ..X..
        ..X..
        ..X..
    """),
    'U' to rows("""
        X...X
        X...X
        X...X
        X...X
        X...X
        X...X
        .XXX.
    """),
    'V' to rows("""
        X...X
        X...X
        X...X
        X...X
        X...X
        .X.X.
        ..X..
    """),
    'W' to rows("""
        X...X
        X...X
        X...X
        X.X.X
        X.X.X
        XX.XX
        X...X
    """),
    'X' to rows("""
        X...X
        X...X
        .X.X.
        ..X..
        .X.X.
        X...X
        X...X
    """),
    'Y' to rows("""
        X...X
        X...X
        .X.X.
        ..X..
        ..X..
        ..X..
        ..X..
    """),
    'Z' to rows("""
        XXXXX
        ....X
        ...X.
        ..X..
        .X...
        X....
        XXXXX
    """),
    '♥' to rows("""
        .X.X.
        XXXXX
        XXXXX
        XXXXX
        .XXX.
        ..X..
        .....
    """),
    ' ' to List(7) { "..." },
)

/** Concatenates glyphs into a 7-row banner with 1px letter spacing. */
private fun textBanner(text: String): List<String> {
    val banner = MutableList(7) { StringBuilder() }
    text.forEach { ch ->
        val glyph = FONT[ch] ?: FONT.getValue(' ')
        for (r in 0 until 7) banner[r].append(glyph[r]).append('.')
    }
    return banner.map { it.toString() }
}

/** Scrolls a sprite through the matrix from the right edge to the left. */
private fun scrollFrames(sprite: List<String>, y: Int): List<IntArray> {
    val width = sprite.maxOf { it.length }
    return (0..width + SIZE).map { step ->
        IntArray(TOTAL).also { blit(it, sprite, SIZE - step, y) }
    }
}

private fun fuckYouFrames(): List<IntArray> =
    scrollFrames(textBanner("FUCK YOU"), y = 3)

/** Frames for a custom scrolling message (A–Z, space and ♥; rest blank). */
fun marqueeFrames(text: String): List<IntArray> =
    scrollFrames(textBanner(text.uppercase()), y = 3)

/**
 * Marquee variant where a diagonal brightness wave travels through the
 * letters while they scroll, so the text shimmers instead of being
 * flat white. Letter pixels swing between 85 and 255.
 */
fun gradientMarqueeFrames(text: String): List<IntArray> {
    val banner = textBanner(text.uppercase())
    val width = banner.maxOf { it.length }
    return (0..width + SIZE).map { step ->
        val frame = IntArray(TOTAL)
        blit(frame, banner, SIZE - step, 3)
        for (i in frame.indices) {
            if (frame[i] > 0) {
                val x = i % SIZE
                val y = i / SIZE
                val phase = 2.0 * PI * (x + y + step * 1.5) / 14.0
                frame[i] = (170 + 85 * sin(phase)).toInt()
            }
        }
        frame
    }
}

/**
 * Kaleidoscope: two brightness waves interfere while their direction
 * slowly rotates a full turn per loop, folded into 4-fold mirror
 * symmetry around the center. Pixels stay put; only brightness moves.
 */
private fun kaleidoscopeFrames(): List<IntArray> {
    val steps = 96
    return (0 until steps).map { t ->
        val turn = 2.0 * PI * t / steps
        IntArray(TOTAL) { i ->
            var x = i % SIZE
            var y = i / SIZE
            if (x > SIZE / 2) x = SIZE - 1 - x
            if (y > SIZE / 2) y = SIZE - 1 - y
            val u = x * cos(turn) + y * sin(turn)
            val w = x * sin(turn) - y * cos(turn)
            val v = sin(2.0 * PI * u / 7 + 3 * turn) + sin(2.0 * PI * w / 9 - 2 * turn)
            (132.5 + 61.0 * v).toInt().coerceIn(10, 255)
        }
    }
}

val PRESETS = listOf(
    Preset("67", sixSevenFrames(), frameMs = 250),
    Preset("FU", fuckYouFrames(), frameMs = 110),
    Preset("BUTT", scrollFrames(textBanner("NICE BUTT ♥"), y = 3), frameMs = 110),
    Preset("Kaleido", kaleidoscopeFrames(), frameMs = 90),
)

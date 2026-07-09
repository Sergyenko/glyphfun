package com.sergy.glyphfun

/**
 * Animated presets for the Glyph Matrix, composed from string-art
 * sprites ('X' bright, 'o' dim, '.' transparent) blitted at offsets.
 */
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
    'F' to rows("""
        XXXXX
        X....
        X....
        XXXX.
        X....
        X....
        X....
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
    'C' to rows("""
        .XXXX
        X....
        X....
        X....
        X....
        X....
        .XXXX
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
    'Y' to rows("""
        X...X
        X...X
        .X.X.
        ..X..
        ..X..
        ..X..
        ..X..
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

val PRESETS = listOf(
    Preset("67", sixSevenFrames(), frameMs = 250),
    Preset("FU", fuckYouFrames(), frameMs = 110),
)

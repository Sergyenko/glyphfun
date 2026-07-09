package com.sergy.glyphfun

/**
 * Animated 13x13 pixel-art presets for the Glyph Matrix.
 *
 * Frames are drawn as string art: 'X' = full brightness, 'o' = dim,
 * '.' = off. Rows shorter than 13 are padded, extra rows ignored,
 * so the art stays hand-editable without crashing on a typo.
 */
data class Preset(val name: String, val frames: List<IntArray>, val frameMs: Long)

private const val SIZE = GlyphGridView.SIZE
private const val BRIGHT = 255
private const val DIM = 110

private fun frame(art: String): IntArray {
    val rows = art.trimIndent().lines()
    val out = IntArray(SIZE * SIZE)
    for (y in 0 until SIZE) {
        val row = rows.getOrNull(y) ?: ""
        for (x in 0 until SIZE) {
            out[y * SIZE + x] = when (row.getOrNull(x)) {
                'X' -> BRIGHT
                'o' -> DIM
                else -> 0
            }
        }
    }
    return out
}

val PRESETS = listOf(

    Preset("Fox", frameMs = 450, frames = listOf(
        frame("""
            .............
            .XX.......XX.
            .XXX.....XXX.
            .XXXX...XXXX.
            .XXXXXXXXXXX.
            .XXXXXXXXXXX.
            .XX.XXXXX.XX.
            .XXXXXXXXXXX.
            ..XXXXXXXXX..
            ...XXXoXXX...
            ....XXXXX....
            .....XXX.....
            ......X......
        """),
        frame("""
            .............
            .XX.......XX.
            .XXX.....XXX.
            .XXXX...XXXX.
            .XXXXXXXXXXX.
            .XXXXXXXXXXX.
            .XXXXXXXXXXX.
            .XXXXXXXXXXX.
            ..XXXXXXXXX..
            ...XXXoXXX...
            ....XXXXX....
            .....XXX.....
            ......X......
        """),
        frame("""
            .............
            .XX.......XX.
            .XXX.....XXX.
            .XXXX...XXXX.
            .XXXXXXXXXXX.
            .XXXXXXXXXXX.
            .XX.XXXXX.XX.
            .XXXXXXXXXXX.
            ..XXXXXXXXX..
            ...XXXoXXX...
            ....XXXXX....
            .....XXX.....
            ......X......
        """),
        frame("""
            .............
            .X.........X.
            .XXX.....XXX.
            .XXXX...XXXX.
            .XXXXXXXXXXX.
            .XXXXXXXXXXX.
            .XX.XXXXX.XX.
            .XXXXXXXXXXX.
            ..XXXXXXXXX..
            ...XXXoXXX...
            ....XXXXX....
            .....XXX.....
            ......X......
        """),
    )),

    Preset("Cat", frameMs = 450, frames = listOf(
        frame("""
            .............
            ..X.......X..
            ..XX.....XX..
            ..XXX...XXX..
            ..XXXXXXXXX..
            ..XXXXXXXXX..
            ..X.XXXXX.X..
            ..XXXXoXXXX..
            X.XXXXXXXXX.X
            .X.XXXXXXX.X.
            ..XXXXXXXXX..
            ...XXXXXXX...
            .............
        """),
        frame("""
            .............
            ..X.......X..
            ..XX.....XX..
            ..XXX...XXX..
            ..XXXXXXXXX..
            ..XXXXXXXXX..
            ..X.XXXXX.X..
            ..XXXXoXXXX..
            .X.XXXXXXX.X.
            X.XXXXXXXXX.X
            ..XXXXXXXXX..
            ...XXXXXXX...
            .............
        """),
        frame("""
            .............
            ..X.......X..
            ..XX.....XX..
            ..XXX...XXX..
            ..XXXXXXXXX..
            ..XXXXXXXXX..
            ..XXXXXXXXX..
            ..XXXXoXXXX..
            X.XXXXXXXXX.X
            .X.XXXXXXX.X.
            ..XXXXXXXXX..
            ...XXXXXXX...
            .............
        """),
    )),

    Preset("Hedgehog", frameMs = 350, frames = listOf(
        frame("""
            .............
            ..X...X...X..
            .X.X.X.X.X...
            .XXXXXXXXX...
            XXXXXXXXXXX..
            XXXXXXXXXXX..
            XXXXXXXXX.XX.
            XXXXXXXXXXXXo
            XXXXXXXXXXXX.
            .XXXXXXXXXX..
            ..XX...XX....
            ..XX...XX....
            .............
        """),
        frame("""
            .............
            ...X...X...X.
            .X.X.X.X.X...
            .XXXXXXXXX...
            XXXXXXXXXXX..
            XXXXXXXXXXX..
            XXXXXXXXX.XX.
            XXXXXXXXXXXXo
            XXXXXXXXXXXX.
            .XXXXXXXXXX..
            .XX.....XX...
            .XX.....XX...
            .............
        """),
    )),

    Preset("Bunny", frameMs = 400, frames = listOf(
        frame("""
            ...XX...XX...
            ...XX...XX...
            ...XX...XX...
            ...XX...XX...
            ..XXXXXXXXX..
            ..X.XXXXX.X..
            ..XXXXoXXXX..
            ..XXXXXXXXX..
            ...XXXXXXX...
            ..XXXXXXXXX..
            ..XXXXXXXXX..
            ...XX...XX...
            .............
        """),
        frame("""
            .............
            ...XX...XX...
            ...XX...XX...
            ...XX...XX...
            ...XX...XX...
            ..XXXXXXXXX..
            ..X.XXXXX.X..
            ..XXXXoXXXX..
            ..XXXXXXXXX..
            ...XXXXXXX...
            ..XXXXXXXXX..
            .XXX.....XXX.
            .............
        """),
    )),

    Preset("Owl", frameMs = 500, frames = listOf(
        frame("""
            .X.........X.
            .XXXXXXXXXXX.
            .XXXXXXXXXXX.
            .XX..XXX..XX.
            .XX..XXX..XX.
            .XXXXXoXXXXX.
            .XXXXXXXXXXX.
            .XXXXXXXXXXX.
            ..XXXXXXXXX..
            ..XXXXXXXXX..
            ...XXXXXXX...
            ....XX.XX....
            .............
        """),
        frame("""
            .X.........X.
            .XXXXXXXXXXX.
            .XXXXXXXXXXX.
            .XXXXXXXXXXX.
            .XX..XXX..XX.
            .XXXXXoXXXXX.
            .XXXXXXXXXXX.
            .XXXXXXXXXXX.
            ..XXXXXXXXX..
            ..XXXXXXXXX..
            ...XXXXXXX...
            ....XX.XX....
            .............
        """),
        frame("""
            .X.........X.
            .XXXXXXXXXXX.
            .XXXXXXXXXXX.
            .XXXXXXXXXXX.
            .XXXXXXXXXXX.
            .XXXXXoXXXXX.
            .XXXXXXXXXXX.
            .XXXXXXXXXXX.
            ..XXXXXXXXX..
            ..XXXXXXXXX..
            ...XXXXXXX...
            ....XX.XX....
            .............
        """),
    )),

    Preset("Heart", frameMs = 300, frames = listOf(
        frame("""
            .............
            ..XXX...XXX..
            .XXXXX.XXXXX.
            .XXXXXXXXXXX.
            .XXXXXXXXXXX.
            ..XXXXXXXXX..
            ...XXXXXXX...
            ....XXXXX....
            .....XXX.....
            ......X......
            .............
            .............
            .............
        """),
        frame("""
            .............
            .............
            ...oo...oo...
            ..oooo.oooo..
            ..ooooooooo..
            ..ooooooooo..
            ...ooooooo...
            ....ooooo....
            .....ooo.....
            ......o......
            .............
            .............
            .............
        """),
    )),
)

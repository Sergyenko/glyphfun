package com.sergy.glyphfun

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

/**
 * A finger-paintable 13x13 grid mirroring the Glyph Matrix.
 * Cells hold brightness 0..255; drawing sets them to full brightness,
 * so a first pass paints and a second tap on a lit cell erases it.
 */
class GlyphGridView(context: Context) : View(context) {

    companion object {
        const val SIZE = 13
    }

    val pixels = IntArray(SIZE * SIZE)
    var onGridChanged: (() -> Unit)? = null

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val offColor = Color.rgb(30, 30, 30)
    private val onColor = Color.WHITE
    private var paintingValue = 255
    private var lastIndex = -1

    fun clear() {
        pixels.fill(0)
        invalidate()
        onGridChanged?.invoke()
    }

    fun setPixels(data: IntArray) {
        data.copyInto(pixels, endIndex = min(data.size, pixels.size))
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, w)
    }

    override fun onDraw(canvas: Canvas) {
        val cell = width / SIZE.toFloat()
        val radius = cell * 0.38f
        for (y in 0 until SIZE) {
            for (x in 0 until SIZE) {
                val v = pixels[y * SIZE + x].coerceIn(0, 255)
                // Mirror LED brightness: off = faint grid dot, lit = gray scale.
                cellPaint.color = if (v == 0) offColor else {
                    val g = 60 + v * 195 / 255
                    Color.rgb(g, g, g)
                }
                canvas.drawCircle(x * cell + cell / 2, y * cell + cell / 2, radius, cellPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val cell = width / SIZE.toFloat()
        val x = (event.x / cell).toInt().coerceIn(0, SIZE - 1)
        val y = (event.y / cell).toInt().coerceIn(0, SIZE - 1)
        val index = y * SIZE + x

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                paintingValue = if (pixels[index] > 0) 0 else 255
                lastIndex = -1
            }
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> Unit
            else -> return false
        }

        if (index != lastIndex && pixels[index] != paintingValue) {
            pixels[index] = paintingValue
            lastIndex = index
            invalidate()
            onGridChanged?.invoke()
        }
        return true
    }
}

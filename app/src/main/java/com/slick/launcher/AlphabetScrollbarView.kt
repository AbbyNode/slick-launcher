package com.slick.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

/**
 * A slim vertical alphabet bar rendered inline in the layout (never overlapping).
 * Letters absent from the app list are omitted. During a drag gesture the active
 * letter is drawn larger with an accent-coloured pill; at rest the currently
 * visible section letter is highlighted at a slightly larger size.
 */
class AlphabetScrollbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnLetterSelectedListener {
        fun onLetterSelected(letter: Char, isDragging: Boolean)
    }

    var letters: List<Char> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    /** Letter that matches the first visible section in the list. */
    var activeLetter: Char? = null
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var listener: OnLetterSelectedListener? = null

    // ── Paint helpers ──────────────────────────────────────────────────────────

    private val sp = { sp: Float ->
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
    }

    private val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = 0xFFEEEEFF.toInt()
        alpha = 90
    }

    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = 0xFFBB86FC.toInt()
        isFakeBoldText = true
    }

    private val dragPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = 0xFFBB86FC.toInt()
        isFakeBoldText = true
    }

    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33BB86FC.toInt()
    }

    private val pillRect = RectF()

    // ── State ──────────────────────────────────────────────────────────────────

    private var isDragging = false
    private var dragLetter: Char? = null

    // ── Drawing ────────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val count = letters.size
        if (count == 0) return

        val cellH = (height - paddingTop - paddingBottom).toFloat() / count
        val cx = width / 2f
        val highlightLetter = if (isDragging) dragLetter else activeLetter

        letters.forEachIndexed { i, letter ->
            val cy = paddingTop + cellH * i + cellH / 2f
            val isHighlighted = letter == highlightLetter

            when {
                isHighlighted && isDragging -> {
                    // Large pill + big text while finger is on screen
                    val textSize = sp(15f)
                    dragPaint.textSize = textSize
                    val halfH = cellH * 0.55f
                    val halfW = width * 0.45f
                    pillRect.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
                    canvas.drawRoundRect(pillRect, halfW, halfW, pillPaint)
                    canvas.drawText(
                        letter.toString(), cx,
                        cy - (dragPaint.ascent() + dragPaint.descent()) / 2f,
                        dragPaint
                    )
                }

                isHighlighted -> {
                    // Slightly larger + accent colour to track current section
                    val textSize = sp(11f)
                    activePaint.textSize = textSize
                    canvas.drawText(
                        letter.toString(), cx,
                        cy - (activePaint.ascent() + activePaint.descent()) / 2f,
                        activePaint
                    )
                }

                else -> {
                    val textSize = sp(9f)
                    normalPaint.textSize = textSize
                    canvas.drawText(
                        letter.toString(), cx,
                        cy - (normalPaint.ascent() + normalPaint.descent()) / 2f,
                        normalPaint
                    )
                }
            }
        }
    }

    // ── Touch ──────────────────────────────────────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                isDragging = true
                val letter = letterAt(event.y)
                if (letter != null && letter != dragLetter) {
                    dragLetter = letter
                    listener?.onLetterSelected(letter, true)
                    invalidate()
                }
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                dragLetter?.let { listener?.onLetterSelected(it, false) }
                dragLetter = null
                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun letterAt(y: Float): Char? {
        if (letters.isEmpty()) return null
        val cellH = (height - paddingTop - paddingBottom).toFloat() / letters.size
        val idx = ((y - paddingTop) / cellH).toInt().coerceIn(0, letters.size - 1)
        return letters[idx]
    }
}

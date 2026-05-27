package com.odysee.app.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.media3.ui.TimeBar
import java.util.concurrent.CopyOnWriteArraySet

class GradientTimeBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr), TimeBar {

    private val density = context.resources.displayMetrics.density
    private val barHeightPx = 4f * density
    private val scrubberRadiusPx = 7f * density
    private val scrubberDraggedRadiusPx = 10f * density
    private val touchTargetPx = (32f * density).toInt()
    private val sidePadPx = scrubberDraggedRadiusPx

    private val unplayedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33FFFFFF }
    private val bufferedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55FFFFFF }
    private val playedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scrubberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF77937.toInt() }

    private var position = 0L
    private var bufferedPosition = 0L
    private var duration = 0L
    private var isDragging = false
    private var dragPosition = 0L

    private val barRect = RectF()

    private val listeners = CopyOnWriteArraySet<TimeBar.OnScrubListener>()

    override fun addListener(listener: TimeBar.OnScrubListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: TimeBar.OnScrubListener) {
        listeners.remove(listener)
    }

    override fun setKeyTimeIncrement(time: Long) {}
    override fun setKeyCountIncrement(count: Int) {}

    override fun setPosition(position: Long) {
        if (!isDragging) {
            this.position = position
            invalidate()
        }
    }

    override fun setBufferedPosition(bufferedPosition: Long) {
        this.bufferedPosition = bufferedPosition
        invalidate()
    }

    override fun setDuration(duration: Long) {
        this.duration = duration
        invalidate()
    }

    override fun getPreferredUpdateDelay(): Long = 50L

    override fun setAdGroupTimesMs(
        adGroupTimesMs: LongArray?,
        playedAdGroups: BooleanArray?,
        adGroupCount: Int,
    ) {}

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, touchTargetPx)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = (right - left).toFloat()
        val height = (bottom - top).toFloat()
        val centerY = height / 2f
        val barTop = centerY - barHeightPx / 2f
        val barBottom = centerY + barHeightPx / 2f
        barRect.set(sidePadPx, barTop, width - sidePadPx, barBottom)
    }

    override fun onDraw(canvas: Canvas) {
        if (barRect.width() <= 0f) return

        val effectivePosition = if (isDragging) dragPosition else position
        val playedFraction =
            if (duration > 0) (effectivePosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            else 0f
        val bufferedFraction =
            if (duration > 0) (bufferedPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            else 0f

        val radius = barHeightPx / 2f
        // Unplayed track
        canvas.drawRoundRect(barRect, radius, radius, unplayedPaint)
        // Buffered overlay
        if (bufferedFraction > 0f) {
            val bRect = RectF(
                barRect.left,
                barRect.top,
                barRect.left + bufferedFraction * barRect.width(),
                barRect.bottom,
            )
            canvas.drawRoundRect(bRect, radius, radius, bufferedPaint)
        }
        // Played gradient track — shader is mapped to the played width so the
        // orange end always reaches the scrubber, mirroring the web's gradient.
        if (playedFraction > 0f) {
            val playedRight = barRect.left + playedFraction * barRect.width()
            val pRect = RectF(barRect.left, barRect.top, playedRight, barRect.bottom)
            playedPaint.shader = LinearGradient(
                barRect.left,
                0f,
                playedRight,
                0f,
                0xFFE50054.toInt(),
                0xFFF77937.toInt(),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRoundRect(pRect, radius, radius, playedPaint)
        }
        // Scrubber
        if (duration > 0) {
            val scrubberX = barRect.left + playedFraction * barRect.width()
            val scrubberY = (barRect.top + barRect.bottom) / 2f
            val r = if (isDragging) scrubberDraggedRadiusPx else scrubberRadiusPx
            canvas.drawCircle(scrubberX, scrubberY, r, scrubberPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (duration <= 0) return false
        val x = event.x
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                parent?.requestDisallowInterceptTouchEvent(true)
                dragPosition = positionFromX(x)
                listeners.forEach { it.onScrubStart(this, dragPosition) }
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    dragPosition = positionFromX(x)
                    listeners.forEach { it.onScrubMove(this, dragPosition) }
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    val cancelled = event.action == MotionEvent.ACTION_CANCEL
                    parent?.requestDisallowInterceptTouchEvent(false)
                    listeners.forEach { it.onScrubStop(this, dragPosition, cancelled) }
                    invalidate()
                    return true
                }
            }
        }
        return false
    }

    private fun positionFromX(x: Float): Long {
        val clamped = x.coerceIn(barRect.left, barRect.right)
        val fraction = (clamped - barRect.left) / barRect.width()
        return (fraction * duration).toLong()
    }
}

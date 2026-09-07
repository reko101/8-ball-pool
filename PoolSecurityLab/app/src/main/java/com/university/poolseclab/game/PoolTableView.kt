package com.university.poolseclab.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws the table and drives the simulation clock.
 *
 * The view keeps no game data of its own. It renders whatever is inside the
 * [PoolGame] it was attached to, which is the same object the Security Lab
 * screens read from.
 */
class PoolTableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var game: PoolGame? = null

    /** Invoked on the UI thread once every ball has come to rest. */
    var onShotFinished: (() -> Unit)? = null

    /** Invoked while the player drags to aim. Argument is the angle in degrees. */
    var onAimChanged: ((Float) -> Unit)? = null

    private var lastFrameNs = 0L
    private var wasMoving = false

    private var scale = 1f
    private var originX = 0f
    private var originY = 0f

    private val feltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1B6B45") }
    private val railPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4E342E") }
    private val railEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#2A1712")
    }
    private val pocketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0C0C0C") }
    private val ballPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ballEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#22000000")
    }
    private val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val stripeClip = Path()
    private val aimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#CCFFFFFF")
    }
    private val ghostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#88FFFFFF")
    }
    private val rect = RectF()

    /** Colour of each numbered ball. Index is the ball number. */
    private val ballColors = intArrayOf(
        Color.parseColor("#F7F7F2"), // 0 cue
        Color.parseColor("#E8C41F"), // 1
        Color.parseColor("#1E5FB4"), // 2
        Color.parseColor("#C62828"), // 3
        Color.parseColor("#6A3AB2"), // 4
        Color.parseColor("#E77817"), // 5
        Color.parseColor("#1F8A4C"), // 6
        Color.parseColor("#8C2F1E"), // 7
        Color.parseColor("#101010"), // 8
        Color.parseColor("#E8C41F"), // 9
        Color.parseColor("#1E5FB4"), // 10
        Color.parseColor("#C62828"), // 11
        Color.parseColor("#6A3AB2"), // 12
        Color.parseColor("#E77817"), // 13
        Color.parseColor("#1F8A4C"), // 14
        Color.parseColor("#8C2F1E")  // 15
    )

    fun attach(g: PoolGame) {
        game = g
        invalidate()
    }

    fun isMoving(): Boolean = game?.engine?.anyMoving() == true

    /** Starts the animation loop after a shot has been played. */
    fun kick() {
        lastFrameNs = 0L
        wasMoving = true
        postInvalidateOnAnimation()
    }

    // -----------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val g = game ?: return

        computeTransform()

        val now = System.nanoTime()
        if (lastFrameNs == 0L) lastFrameNs = now
        val dt = ((now - lastFrameNs) / 1_000_000_000.0).toFloat().coerceIn(0f, 0.05f)
        lastFrameNs = now

        if (g.engine.anyMoving()) {
            g.engine.step(dt)
        }

        drawTable(canvas)
        drawBalls(canvas, g)
        if (!g.engine.anyMoving() && !g.isFinished()) {
            drawAim(canvas, g)
        }

        if (g.engine.anyMoving()) {
            postInvalidateOnAnimation()
        } else if (wasMoving) {
            wasMoving = false
            lastFrameNs = 0L
            post { onShotFinished?.invoke() }
        }
    }

    private fun computeTransform() {
        val totalW = TableSpec.WIDTH + 2f * TableSpec.RAIL
        val totalH = TableSpec.HEIGHT + 2f * TableSpec.RAIL
        val pad = 8f
        val availW = (width - 2f * pad).coerceAtLeast(1f)
        val availH = (height - 2f * pad).coerceAtLeast(1f)
        scale = min(availW / totalW, availH / totalH)
        originX = (width - totalW * scale) / 2f + TableSpec.RAIL * scale
        originY = (height - totalH * scale) / 2f + TableSpec.RAIL * scale
    }

    private fun sx(x: Float) = originX + x * scale
    private fun sy(y: Float) = originY + y * scale

    private fun drawTable(canvas: Canvas) {
        rect.set(
            sx(-TableSpec.RAIL), sy(-TableSpec.RAIL),
            sx(TableSpec.WIDTH + TableSpec.RAIL), sy(TableSpec.HEIGHT + TableSpec.RAIL)
        )
        val r = 6f * scale
        canvas.drawRoundRect(rect, r, r, railPaint)
        railEdgePaint.strokeWidth = 1.5f * scale
        canvas.drawRoundRect(rect, r, r, railEdgePaint)

        rect.set(sx(0f), sy(0f), sx(TableSpec.WIDTH), sy(TableSpec.HEIGHT))
        canvas.drawRect(rect, feltPaint)

        for (p in TableSpec.POCKETS) {
            canvas.drawCircle(sx(p[0]), sy(p[1]), TableSpec.POCKET_R * scale, pocketPaint)
        }
    }

    private fun drawBalls(canvas: Canvas, g: PoolGame) {
        val r = TableSpec.BALL_R * scale
        numberPaint.textSize = r * 0.95f
        ballEdgePaint.strokeWidth = 1f.coerceAtLeast(r * 0.08f)

        for (b in g.state.balls) {
            if (b.potted) continue
            val cx = sx(b.x)
            val cy = sy(b.y)
            val colour = ballColors[b.number.coerceIn(0, 15)]

            if (b.number in 9..15) {
                // Striped ball: white body with a coloured band across the middle.
                ballPaint.color = Color.parseColor("#F7F7F2")
                canvas.drawCircle(cx, cy, r, ballPaint)
                canvas.save()
                stripeClip.reset()
                stripeClip.addCircle(cx, cy, r, Path.Direction.CW)
                canvas.clipPath(stripeClip)
                ballPaint.color = colour
                canvas.drawRect(cx - r, cy - r * 0.52f, cx + r, cy + r * 0.52f, ballPaint)
                canvas.restore()
            } else {
                ballPaint.color = colour
                canvas.drawCircle(cx, cy, r, ballPaint)
            }

            canvas.drawCircle(cx, cy, r, ballEdgePaint)

            if (!b.isCue) {
                ballPaint.color = Color.parseColor("#F7F7F2")
                canvas.drawCircle(cx, cy, r * 0.52f, ballPaint)
                numberPaint.color = Color.parseColor("#101010")
                canvas.drawText(
                    b.number.toString(),
                    cx,
                    cy + numberPaint.textSize * 0.35f,
                    numberPaint
                )
            }
        }
    }

    private fun drawAim(canvas: Canvas, g: PoolGame) {
        val cue = g.state.cueBall
        if (cue.potted) return

        val rad = Math.toRadians(g.state.shotAngleDeg.toDouble())
        val dx = cos(rad).toFloat()
        val dy = sin(rad).toFloat()
        val dist = g.engine.firstObstacleDistance(dx, dy)

        aimPaint.strokeWidth = 1.4f.coerceAtLeast(scale * 0.5f)
        aimPaint.pathEffect = DashPathEffect(floatArrayOf(6f * scale * 0.4f, 4f * scale * 0.4f), 0f)
        canvas.drawLine(
            sx(cue.x), sy(cue.y),
            sx(cue.x + dx * dist), sy(cue.y + dy * dist),
            aimPaint
        )

        ghostPaint.strokeWidth = 1.2f.coerceAtLeast(scale * 0.4f)
        canvas.drawCircle(
            sx(cue.x + dx * dist),
            sy(cue.y + dy * dist),
            TableSpec.BALL_R * scale,
            ghostPaint
        )
    }

    // -----------------------------------------------------------------
    // Aiming input
    // -----------------------------------------------------------------

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val g = game ?: return false
        if (g.engine.anyMoving() || g.isFinished()) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                updateAim(g, event.x, event.y)
                return true
            }
            MotionEvent.ACTION_UP -> {
                updateAim(g, event.x, event.y)
                performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> return true
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun updateAim(g: PoolGame, px: Float, py: Float) {
        if (scale <= 0f) return
        val wx = (px - originX) / scale
        val wy = (py - originY) / scale
        val cue = g.state.cueBall
        val ddx = wx - cue.x
        val ddy = wy - cue.y
        if (ddx * ddx + ddy * ddy < 0.5f) return
        val angle = Math.toDegrees(atan2(ddy.toDouble(), ddx.toDouble())).toFloat()
        g.state.shotAngleDeg = angle
        onAimChanged?.invoke(angle)
        invalidate()
    }
}

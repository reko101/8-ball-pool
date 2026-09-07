package com.university.poolseclab.game

import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sqrt

/**
 * A small fixed step physics solver: straight line motion, rolling friction,
 * equal mass ball to ball impacts, cushion bounces and pocket capture.
 *
 * The solver is deterministic. Given the same starting state and the same shot
 * it always produces the same result, which is what makes it usable as a
 * teaching bench for state integrity.
 */
class PhysicsEngine(private val state: GameState) {

    /** Numbers of the balls pocketed during the shot currently being simulated. */
    val pottedThisShot: MutableList<Int> = ArrayList()

    var cueBallPotted: Boolean = false
        private set

    /** Number of the first object ball the cue ball touched, or -1. */
    var firstContact: Int = -1
        private set

    fun resetShotRecord() {
        pottedThisShot.clear()
        cueBallPotted = false
        firstContact = -1
    }

    fun anyMoving(): Boolean {
        for (b in state.balls) {
            if (b.isMoving()) return true
        }
        return false
    }

    /** Advances the simulation by [dtSeconds] of wall clock time. */
    fun step(dtSeconds: Float) {
        var remaining = dtSeconds.coerceIn(0f, 0.05f)
        val h = 1f / 480f
        while (remaining > 0f) {
            val slice = min(remaining, h)
            substep(slice)
            remaining -= slice
        }
        state.lastUpdateMillis = System.currentTimeMillis()
    }

    private fun substep(h: Float) {
        integrate(h)
        resolveBallCollisions()
        resolvePocketsAndCushions()
    }

    private fun integrate(h: Float) {
        for (b in state.balls) {
            if (b.potted) continue
            b.x += b.vx * h
            b.y += b.vy * h

            val sp = hypot(b.vx, b.vy)
            if (sp > 0f) {
                val newSp = sp - TableSpec.FRICTION * h
                if (newSp <= TableSpec.MIN_SPEED) {
                    b.stop()
                } else {
                    val k = newSp / sp
                    b.vx *= k
                    b.vy *= k
                }
            }
        }
    }

    private fun resolveBallCollisions() {
        val list = state.balls
        val diameter = 2f * TableSpec.BALL_R
        for (i in list.indices) {
            val a = list[i]
            if (a.potted) continue
            for (j in i + 1 until list.size) {
                val b = list[j]
                if (b.potted) continue

                var dx = b.x - a.x
                var dy = b.y - a.y
                var dist = hypot(dx, dy)
                if (dist >= diameter) continue

                if (dist == 0f) {
                    // Perfectly coincident centres: nudge them apart deterministically.
                    dx = 0.001f
                    dy = 0f
                    dist = 0.001f
                }

                val nx = dx / dist
                val ny = dy / dist

                // Positional correction so the balls stop overlapping.
                val overlap = (diameter - dist) * 0.5f
                a.x -= nx * overlap
                a.y -= ny * overlap
                b.x += nx * overlap
                b.y += ny * overlap

                // Relative velocity along the contact normal.
                val rvx = b.vx - a.vx
                val rvy = b.vy - a.vy
                val sep = rvx * nx + rvy * ny
                if (sep >= 0f) continue

                if (firstContact == -1 && (a.isCue || b.isCue)) {
                    firstContact = if (a.isCue) b.number else a.number
                }

                // Equal masses, so the impulse splits evenly.
                val impulse = -(1f + TableSpec.BALL_RESTITUTION) * sep / 2f
                a.vx -= nx * impulse
                a.vy -= ny * impulse
                b.vx += nx * impulse
                b.vy += ny * impulse
            }
        }
    }

    private fun resolvePocketsAndCushions() {
        for (b in state.balls) {
            if (b.potted) continue

            // Pocket capture is tested before the cushion so that a ball rolling
            // along a rail can still drop into the mouth of a pocket.
            var captured = false
            for (p in TableSpec.POCKETS) {
                val dx = b.x - p[0]
                val dy = b.y - p[1]
                if (hypot(dx, dy) < TableSpec.POCKET_R) {
                    captured = true
                    break
                }
            }
            if (captured) {
                b.potted = true
                b.stop()
                if (b.isCue) cueBallPotted = true else pottedThisShot.add(b.number)
                continue
            }

            val r = TableSpec.BALL_R
            if (b.x < r) {
                b.x = r
                if (b.vx < 0f) b.vx = -b.vx * TableSpec.CUSHION_RESTITUTION
            } else if (b.x > TableSpec.WIDTH - r) {
                b.x = TableSpec.WIDTH - r
                if (b.vx > 0f) b.vx = -b.vx * TableSpec.CUSHION_RESTITUTION
            }
            if (b.y < r) {
                b.y = r
                if (b.vy < 0f) b.vy = -b.vy * TableSpec.CUSHION_RESTITUTION
            } else if (b.y > TableSpec.HEIGHT - r) {
                b.y = TableSpec.HEIGHT - r
                if (b.vy > 0f) b.vy = -b.vy * TableSpec.CUSHION_RESTITUTION
            }
        }
    }

    /**
     * Distance from the cue ball to the first thing a straight shot would touch.
     * Used only to draw the aiming guide, exactly like the guide line that every
     * pool game draws for its own table.
     */
    fun firstObstacleDistance(dirX: Float, dirY: Float): Float {
        val cue = state.cueBall
        var best = Float.MAX_VALUE
        val contactDist = 2f * TableSpec.BALL_R

        for (b in state.balls) {
            if (b === cue || b.potted) continue
            val ex = b.x - cue.x
            val ey = b.y - cue.y
            val t = ex * dirX + ey * dirY
            if (t <= 0f) continue
            val perp2 = ex * ex + ey * ey - t * t
            val c2 = contactDist * contactDist
            if (perp2 > c2) continue
            val back = sqrt(c2 - perp2)
            val hit = t - back
            if (hit in 0f..best) best = hit
        }

        val r = TableSpec.BALL_R
        if (dirX > 0.0001f) best = min(best, (TableSpec.WIDTH - r - cue.x) / dirX)
        if (dirX < -0.0001f) best = min(best, (r - cue.x) / dirX)
        if (dirY > 0.0001f) best = min(best, (TableSpec.HEIGHT - r - cue.y) / dirY)
        if (dirY < -0.0001f) best = min(best, (r - cue.y) / dirY)

        return if (best == Float.MAX_VALUE) 40f else best.coerceIn(0f, 400f)
    }
}

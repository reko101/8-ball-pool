package com.university.poolseclab.game

import kotlin.math.hypot

/** Which half of the object balls a ball belongs to. */
enum class BallGroup { UNASSIGNED, SOLIDS, STRIPES, EIGHT }

/**
 * One ball on the simulated table.
 *
 * Number 0 is the cue ball, 1 to 7 are solids, 8 is the black ball and
 * 9 to 15 are stripes.
 *
 * Note for the laboratory: the fields below are ordinary mutable properties.
 * That is deliberate. The Security Lab screens show what happens when a value
 * like this is the only copy of the truth and anybody who can reach the process
 * can change it.
 */
class Ball(val number: Int, var x: Float, var y: Float) {

    var vx: Float = 0f
    var vy: Float = 0f
    var potted: Boolean = false

    val isCue: Boolean get() = number == 0

    val group: BallGroup
        get() = when (number) {
            0 -> BallGroup.UNASSIGNED
            in 1..7 -> BallGroup.SOLIDS
            8 -> BallGroup.EIGHT
            else -> BallGroup.STRIPES
        }

    fun speed(): Float = hypot(vx, vy)

    fun stop() {
        vx = 0f
        vy = 0f
    }

    fun isMoving(): Boolean = !potted && (vx != 0f || vy != 0f)
}

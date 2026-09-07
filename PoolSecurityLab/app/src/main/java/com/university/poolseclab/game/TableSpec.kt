package com.university.poolseclab.game

/**
 * Geometry and physical constants of the simulated table.
 *
 * All units are "table units". The playing surface is 200 x 100 units, which is
 * the 2:1 ratio of a real pool table. The renderer scales these units to pixels,
 * so the simulation itself is completely resolution independent.
 */
object TableSpec {

    const val WIDTH = 200f
    const val HEIGHT = 100f

    /** Radius of every ball. */
    const val BALL_R = 2.35f

    /** Capture radius of a pocket, measured from the pocket centre. */
    const val POCKET_R = 4.6f

    /** Width of the wooden rail drawn around the playing surface. */
    const val RAIL = 7f

    /** Rolling deceleration in units per second squared. */
    const val FRICTION = 26f

    /** Energy kept after bouncing off a cushion. */
    const val CUSHION_RESTITUTION = 0.86f

    /** Energy kept in a ball to ball impact. */
    const val BALL_RESTITUTION = 0.95f

    /** Below this speed a ball is considered stopped. */
    const val MIN_SPEED = 1.5f

    /** Speed produced by a shot at 100 percent power. */
    const val MAX_SPEED = 260f

    /** Head spot where the cue ball is re-spotted after a foul. */
    const val CUE_SPOT_X = 50f
    const val CUE_SPOT_Y = 50f

    /** Six pockets: four corners and two in the middle of the long cushions. */
    val POCKETS: Array<FloatArray> = arrayOf(
        floatArrayOf(0f, 0f),
        floatArrayOf(WIDTH / 2f, 0f),
        floatArrayOf(WIDTH, 0f),
        floatArrayOf(0f, HEIGHT),
        floatArrayOf(WIDTH / 2f, HEIGHT),
        floatArrayOf(WIDTH, HEIGHT)
    )
}

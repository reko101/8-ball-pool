package com.university.poolseclab.game

import java.util.Locale
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * The rules layer. It owns a [GameState] and a [PhysicsEngine] and turns the
 * outcome of a shot into turns, scores and a win or loss.
 *
 * The rule set is a simplified eight ball game, which is enough to produce a
 * realistic looking client state for the security exercises.
 */
class PoolGame {

    val state = GameState()
    val engine = PhysicsEngine(state)

    /** Coins granted for a win. Also the ceiling used by the impossible value check. */
    val winReward: Long = 100L

    init {
        newGame()
    }

    // -----------------------------------------------------------------
    // Setup
    // -----------------------------------------------------------------

    fun newGame() {
        state.matchId = String.format(Locale.US, "MATCH-%06d", Random.nextInt(1, 999_999))
        state.currentTurn = 1
        state.playerOneGroup = BallGroup.UNASSIGNED
        state.playerTwoGroup = BallGroup.UNASSIGNED
        state.playerOneScore = 0
        state.playerTwoScore = 0
        state.shotCount = 0
        state.shotPower = 0.55f
        state.shotAngleDeg = 0f
        state.matchPhase = MatchPhase.READY
        state.statusMessage = "Break shot. Player 1 to play."
        state.stateSequence++
        rack()
        engine.resetShotRecord()
    }

    private fun rack() {
        state.balls.clear()
        state.balls.add(Ball(0, TableSpec.CUE_SPOT_X, TableSpec.CUE_SPOT_Y))

        // Standard triangle: apex towards the cue ball, black ball in the middle row.
        val rows = arrayOf(
            intArrayOf(1),
            intArrayOf(9, 2),
            intArrayOf(10, 8, 3),
            intArrayOf(11, 4, 12, 5),
            intArrayOf(13, 6, 14, 7, 15)
        )

        val gap = 2f * TableSpec.BALL_R * 1.02f
        val rowStepX = gap * 0.866f      // cos(30 degrees)
        val apexX = 138f
        val centreY = TableSpec.HEIGHT / 2f

        for (r in rows.indices) {
            val row = rows[r]
            val x = apexX + r * rowStepX
            val startY = centreY - (row.size - 1) * gap / 2f
            for (c in row.indices) {
                state.balls.add(Ball(row[c], x, startY + c * gap))
            }
        }
    }

    // -----------------------------------------------------------------
    // Shooting
    // -----------------------------------------------------------------

    fun isFinished(): Boolean =
        state.matchPhase == MatchPhase.PLAYER_ONE_WINS || state.matchPhase == MatchPhase.PLAYER_TWO_WINS

    fun canShoot(): Boolean = !isFinished() && !engine.anyMoving()

    fun shoot(powerFraction: Float, angleDeg: Float) {
        if (!canShoot()) return

        val p = powerFraction.coerceIn(0.05f, 1f)
        state.shotPower = p
        state.shotAngleDeg = angleDeg

        val rad = Math.toRadians(angleDeg.toDouble())
        val cue = state.cueBall
        cue.vx = (cos(rad) * p * TableSpec.MAX_SPEED).toFloat()
        cue.vy = (sin(rad) * p * TableSpec.MAX_SPEED).toFloat()

        state.shotCount++
        state.stateSequence++
        state.matchPhase = MatchPhase.BALLS_MOVING
        engine.resetShotRecord()
    }

    /**
     * Called once, after every ball has come to rest. Applies the rules and
     * returns a human readable summary of what happened.
     */
    fun onBallsStopped(): String {
        val potted = engine.pottedThisShot.toList()
        val cuePotted = engine.cueBallPotted
        val touched = engine.firstContact
        engine.resetShotRecord()

        val shooter = state.currentTurn
        val objectBalls = potted.filter { it != 8 }
        state.stateSequence++

        // First legal pot decides which half belongs to which player.
        if (state.playerOneGroup == BallGroup.UNASSIGNED && objectBalls.isNotEmpty() && !cuePotted) {
            val g = if (objectBalls.first() <= 7) BallGroup.SOLIDS else BallGroup.STRIPES
            if (shooter == 1) {
                state.playerOneGroup = g
                state.playerTwoGroup = opposite(g)
            } else {
                state.playerTwoGroup = g
                state.playerOneGroup = opposite(g)
            }
        }

        recountScores()

        // The black ball ends the match either way.
        if (potted.contains(8)) {
            val myGroup = state.groupOf(shooter)
            val cleared = myGroup != BallGroup.UNASSIGNED && state.remainingIn(myGroup) == 0
            val winner = if (cleared && !cuePotted) shooter else 3 - shooter
            state.matchPhase =
                if (winner == 1) MatchPhase.PLAYER_ONE_WINS else MatchPhase.PLAYER_TWO_WINS
            if (winner == 1) awardCoinsSanctioned(winReward)
            val why = if (cleared && !cuePotted) "cleared the group and potted the black" else "potted the black too early"
            val msg = "Player " + winner + " wins. Player " + shooter + " " + why + "."
            state.statusMessage = msg
            return msg
        }

        val msg: String
        if (cuePotted) {
            respotCueBall()
            switchTurn()
            msg = "Foul: cue ball pocketed. Player " + state.currentTurn + " to play."
        } else if (objectBalls.isEmpty() && touched == -1) {
            switchTurn()
            msg = "Foul: no ball was hit. Player " + state.currentTurn + " to play."
        } else {
            val myGroup = state.groupOf(shooter)
            val pottedOwn = objectBalls.any { numberGroup(it) == myGroup } ||
                    (myGroup == BallGroup.UNASSIGNED && objectBalls.isNotEmpty())
            if (pottedOwn) {
                msg = "Potted. Player " + shooter + " continues."
            } else {
                switchTurn()
                msg = "Player " + state.currentTurn + " to play."
            }
        }

        state.matchPhase = MatchPhase.READY
        state.statusMessage = msg
        return msg
    }

    // -----------------------------------------------------------------
    // Sanctioned mutators
    // -----------------------------------------------------------------

    /**
     * The only legitimate way for the balance to change. Every mutation goes
     * through here so that the integrity guard can distinguish a change made by
     * the rules from a change made by somebody poking at the field directly.
     */
    fun awardCoinsSanctioned(amount: Long) {
        if (amount <= 0L) return
        state.virtualBalance += amount
        state.stateSequence++
        state.lastUpdateMillis = System.currentTimeMillis()
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private fun opposite(g: BallGroup): BallGroup =
        if (g == BallGroup.SOLIDS) BallGroup.STRIPES else BallGroup.SOLIDS

    private fun numberGroup(n: Int): BallGroup = when (n) {
        in 1..7 -> BallGroup.SOLIDS
        8 -> BallGroup.EIGHT
        in 9..15 -> BallGroup.STRIPES
        else -> BallGroup.UNASSIGNED
    }

    private fun switchTurn() {
        state.currentTurn = 3 - state.currentTurn
    }

    private fun recountScores() {
        state.playerOneScore = countPotted(state.playerOneGroup)
        state.playerTwoScore = countPotted(state.playerTwoGroup)
    }

    private fun countPotted(g: BallGroup): Int {
        if (g == BallGroup.UNASSIGNED) return 0
        var n = 0
        for (b in state.balls) {
            if (b.potted && b.group == g) n++
        }
        return n
    }

    private fun respotCueBall() {
        val cue = state.cueBall
        cue.potted = false
        cue.stop()
        var x = TableSpec.CUE_SPOT_X
        val y = TableSpec.CUE_SPOT_Y
        var attempts = 0
        while (attempts < 40 && occupied(x, y)) {
            x -= 3f * TableSpec.BALL_R
            if (x < TableSpec.BALL_R * 2f) x = TableSpec.CUE_SPOT_X + 3f * TableSpec.BALL_R * attempts
            attempts++
        }
        cue.x = x.coerceIn(TableSpec.BALL_R, TableSpec.WIDTH - TableSpec.BALL_R)
        cue.y = y
    }

    private fun occupied(x: Float, y: Float): Boolean {
        for (b in state.balls) {
            if (b.isCue || b.potted) continue
            if (hypot(b.x - x, b.y - y) < 2.5f * TableSpec.BALL_R) return true
        }
        return false
    }
}

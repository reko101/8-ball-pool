package com.university.poolseclab.game

/** Phase of the current match. */
enum class MatchPhase { READY, BALLS_MOVING, PLAYER_ONE_WINS, PLAYER_TWO_WINS }

/**
 * The complete client side state of a match.
 *
 * This single object is the subject of the whole laboratory. Everything the
 * Security Lab, the memory map and the tamper demonstration show is read from
 * here, and nowhere else. No other process is ever inspected.
 */
class GameState {

    // ---- Identity -------------------------------------------------------
    var playerId: String = "STUDENT-LAB-0001"
    var playerName: String = "Lab Player 1"
    var matchId: String = "MATCH-000000"

    // ---- Turn and groups ------------------------------------------------
    var currentTurn: Int = 1                       // 1 or 2
    var playerOneGroup: BallGroup = BallGroup.UNASSIGNED
    var playerTwoGroup: BallGroup = BallGroup.UNASSIGNED

    // ---- Scores and virtual economy -------------------------------------
    var playerOneScore: Int = 0
    var playerTwoScore: Int = 0
    var virtualBalance: Long = 1000L               // simulated in-app coins

    // ---- Last shot ------------------------------------------------------
    var shotPower: Float = 0.55f                   // 0.0 .. 1.0
    var shotAngleDeg: Float = 0f
    var shotCount: Int = 0

    // ---- Match bookkeeping ----------------------------------------------
    var matchPhase: MatchPhase = MatchPhase.READY
    var statusMessage: String = "Break shot."

    /**
     * Monotonically increasing counter. Every sanctioned mutation increments it.
     * A value that goes backwards is the signal used by the replay detector.
     */
    var stateSequence: Long = 0L

    var lastUpdateMillis: Long = System.currentTimeMillis()

    /** Index 0 is always the cue ball; indices 1..15 are the object balls. */
    val balls: MutableList<Ball> = ArrayList(16)

    val cueBall: Ball get() = balls[0]

    fun groupOf(player: Int): BallGroup =
        if (player == 1) playerOneGroup else playerTwoGroup

    fun scoreOf(player: Int): Int =
        if (player == 1) playerOneScore else playerTwoScore

    fun remainingIn(group: BallGroup): Int {
        if (group == BallGroup.UNASSIGNED) return 7
        var n = 0
        for (b in balls) {
            if (!b.potted && b.group == group) n++
        }
        return n
    }

    fun ballByNumber(number: Int): Ball? = balls.firstOrNull { it.number == number }

    fun maxBallSpeed(): Float {
        var m = 0f
        for (b in balls) {
            if (!b.potted && b.speed() > m) m = b.speed()
        }
        return m
    }
}

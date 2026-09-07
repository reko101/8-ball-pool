package com.university.poolseclab.security

import com.university.poolseclab.game.GameState
import com.university.poolseclab.game.TableSpec
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** One thing that failed a check. */
data class IntegrityFinding(
    val field: String,
    val originalValue: String,
    val currentValue: String,
    val rule: String
)

/** Result of a full verification pass. */
data class IntegrityReport(
    val ok: Boolean,
    val findings: List<IntegrityFinding>,
    val detectedAt: String,
    val expectedTag: String,
    val actualTag: String,
    val sealedSequence: Long,
    val currentSequence: Long,
    val neverSealed: Boolean
)

/**
 * Demonstrates client side integrity checking over the application own state.
 *
 * WHAT IS REAL HERE
 *   The hashing, the HMAC, the range checks, the impossible value checks and
 *   the monotonic counter check are all genuinely computed.
 *
 * WHAT IS ONLY A DEMONSTRATION
 *   The key lives in this process. Anybody who can modify the state can also
 *   read the key and forge a fresh tag. That is not a flaw in the code, it is
 *   the actual lesson: a client cannot verify itself against its own owner.
 *   The same construction becomes meaningful only when the key and the check
 *   live on a machine the player does not control. See the Security Report.
 */
class IntegrityGuard(private val state: GameState) {

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    /** Session key, generated at start up and never persisted or transmitted. */
    private val key: ByteArray = ByteArray(32).also { SecureRandom().nextBytes(it) }

    private var sealedTag: String = ""
    private var sealedValues: LinkedHashMap<String, String> = LinkedHashMap()
    private var snapshot: Snapshot? = null

    val isSealed: Boolean get() = snapshot != null
    var sealedAtText: String = "never"
        private set

    /** Upper bound used by the range check on the simulated currency. */
    private val maxReasonableBalance = 100_000L

    /** Largest balance increase that one match can legitimately produce. */
    private val maxBalanceGainPerMatch = 100L

    private class Snapshot(
        val balance: Long,
        val scoreOne: Int,
        val scoreTwo: Int,
        val turn: Int,
        val sequence: Long,
        val phase: String,
        val ballX: FloatArray,
        val ballY: FloatArray,
        val potted: BooleanArray
    )

    // -----------------------------------------------------------------
    // Canonical serialisation and tagging
    // -----------------------------------------------------------------

    /**
     * Deterministic text form of the protected part of the state. Canonical
     * means: same state, same bytes, every time, on every device. Without that
     * property a signature over the data is meaningless.
     */
    fun canonicalState(): String {
        val sb = StringBuilder(256)
        sb.append("v1|")
        sb.append("player=").append(state.playerId).append('|')
        sb.append("match=").append(state.matchId).append('|')
        sb.append("turn=").append(state.currentTurn).append('|')
        sb.append("s1=").append(state.playerOneScore).append('|')
        sb.append("s2=").append(state.playerTwoScore).append('|')
        sb.append("balance=").append(state.virtualBalance).append('|')
        sb.append("phase=").append(state.matchPhase.name).append('|')
        sb.append("seq=").append(state.stateSequence).append('|')
        for (b in state.balls) {
            sb.append('b').append(b.number).append('=')
                .append(String.format(Locale.US, "%.3f,%.3f,%b", b.x, b.y, b.potted))
                .append(';')
        }
        return sb.toString()
    }

    /** SHA-256 based HMAC over the canonical state. */
    fun hmac(payload: String): String {
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(key, "HmacSHA256"))
            val raw = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
            val sb = StringBuilder(raw.size * 2)
            for (byte in raw) {
                sb.append(String.format(Locale.US, "%02x", byte))
            }
            sb.toString()
        } catch (e: Exception) {
            "unavailable:" + e.javaClass.simpleName
        }
    }

    fun currentTag(): String = hmac(canonicalState())

    private fun protectedValues(): LinkedHashMap<String, String> {
        val m = LinkedHashMap<String, String>()
        m["GameState -> Player -> Balance"] = state.virtualBalance.toString()
        m["GameState -> Player -> Score"] = state.playerOneScore.toString()
        m["GameState -> Opponent -> Score"] = state.playerTwoScore.toString()
        m["GameState -> Match -> CurrentTurn"] = state.currentTurn.toString()
        m["GameState -> Match -> Phase"] = state.matchPhase.name
        m["GameState -> Sequence"] = state.stateSequence.toString()
        m["GameState -> CueBall -> Position"] =
            String.format(Locale.US, "(%.2f, %.2f)", state.cueBall.x, state.cueBall.y)
        m["GameState -> MaxBallSpeed"] =
            String.format(Locale.US, "%.2f", state.maxBallSpeed())
        return m
    }

    // -----------------------------------------------------------------
    // Sealing and verification
    // -----------------------------------------------------------------

    /** Records the current state as the trusted baseline. */
    fun seal() {
        sealedTag = currentTag()
        sealedValues = protectedValues()
        snapshot = Snapshot(
            balance = state.virtualBalance,
            scoreOne = state.playerOneScore,
            scoreTwo = state.playerTwoScore,
            turn = state.currentTurn,
            sequence = state.stateSequence,
            phase = state.matchPhase.name,
            ballX = FloatArray(state.balls.size) { state.balls[it].x },
            ballY = FloatArray(state.balls.size) { state.balls[it].y },
            potted = BooleanArray(state.balls.size) { state.balls[it].potted }
        )
        sealedAtText = timeFormat.format(Date())
    }

    /** Runs every check and reports what, if anything, changed without permission. */
    fun verify(): IntegrityReport {
        val now = timeFormat.format(Date())
        val snap = snapshot
            ?: return IntegrityReport(
                ok = false,
                findings = emptyList(),
                detectedAt = now,
                expectedTag = "-",
                actualTag = "-",
                sealedSequence = 0L,
                currentSequence = state.stateSequence,
                neverSealed = true
            )

        val findings = ArrayList<IntegrityFinding>()
        val actual = currentTag()
        val current = protectedValues()

        // 1. Cryptographic check over the whole protected payload.
        if (actual != sealedTag) {
            for ((field, sealedValue) in sealedValues) {
                val nowValue = current[field] ?: "?"
                if (nowValue != sealedValue) {
                    findings.add(
                        IntegrityFinding(field, sealedValue, nowValue, "HMAC-SHA256 payload mismatch")
                    )
                }
            }
            if (findings.isEmpty()) {
                findings.add(
                    IntegrityFinding(
                        "GameState (whole payload)", sealedTag.take(16), actual.take(16),
                        "HMAC-SHA256 tag mismatch"
                    )
                )
            }
        }

        // 2. Range check on the simulated currency.
        if (state.virtualBalance < 0L || state.virtualBalance > maxReasonableBalance) {
            findings.add(
                IntegrityFinding(
                    "GameState -> Player -> Balance",
                    snap.balance.toString(),
                    state.virtualBalance.toString(),
                    "Range check: allowed 0 .. " + maxReasonableBalance
                )
            )
        }

        // 3. Impossible value check: no single match can pay more than the reward.
        val gain = state.virtualBalance - snap.balance
        if (gain > maxBalanceGainPerMatch) {
            findings.add(
                IntegrityFinding(
                    "GameState -> Player -> Balance",
                    snap.balance.toString(),
                    state.virtualBalance.toString(),
                    "Impossible gain: +" + gain + " exceeds the maximum of " + maxBalanceGainPerMatch + " per match"
                )
            )
        }

        // 4. Range check on the scores.
        if (state.playerOneScore !in 0..7 || state.playerTwoScore !in 0..7) {
            findings.add(
                IntegrityFinding(
                    "GameState -> Player -> Score",
                    snap.scoreOne.toString() + " / " + snap.scoreTwo,
                    state.playerOneScore.toString() + " / " + state.playerTwoScore,
                    "Range check: a group holds at most 7 balls"
                )
            )
        }

        // 5. Turn must be a valid seat.
        if (state.currentTurn != 1 && state.currentTurn != 2) {
            findings.add(
                IntegrityFinding(
                    "GameState -> Match -> CurrentTurn",
                    snap.turn.toString(),
                    state.currentTurn.toString(),
                    "Domain check: turn must be 1 or 2"
                )
            )
        }

        // 6. Geometry check: every ball must be on the table.
        for (b in state.balls) {
            if (b.potted) continue
            val outside = b.x < -1f || b.x > TableSpec.WIDTH + 1f ||
                    b.y < -1f || b.y > TableSpec.HEIGHT + 1f
            if (outside) {
                findings.add(
                    IntegrityFinding(
                        "GameState -> Balls[" + b.number + "] -> Position",
                        "inside the 200 x 100 playing surface",
                        String.format(Locale.US, "(%.2f, %.2f)", b.x, b.y),
                        "Geometry check: position is off the table"
                    )
                )
            }
        }

        // 7. Physics check: no ball can exceed the maximum speed of the simulation.
        val limit = TableSpec.MAX_SPEED * 1.05f
        for (b in state.balls) {
            if (b.potted) continue
            if (b.speed() > limit) {
                findings.add(
                    IntegrityFinding(
                        "GameState -> Balls[" + b.number + "] -> Velocity",
                        String.format(Locale.US, "at most %.0f", limit),
                        String.format(Locale.US, "%.0f", b.speed()),
                        "Physics check: speed above the simulation maximum"
                    )
                )
            }
        }

        // 8. Replay check: the sequence counter must never move backwards.
        if (state.stateSequence < snap.sequence) {
            findings.add(
                IntegrityFinding(
                    "GameState -> Sequence",
                    snap.sequence.toString(),
                    state.stateSequence.toString(),
                    "Replay check: monotonic counter moved backwards"
                )
            )
        }

        return IntegrityReport(
            ok = findings.isEmpty(),
            findings = findings,
            detectedAt = now,
            expectedTag = sealedTag,
            actualTag = actual,
            sealedSequence = snap.sequence,
            currentSequence = state.stateSequence,
            neverSealed = false
        )
    }

    /** Puts the sealed baseline back, the way a server would resend authoritative state. */
    fun restore() {
        val snap = snapshot ?: return
        state.virtualBalance = snap.balance
        state.playerOneScore = snap.scoreOne
        state.playerTwoScore = snap.scoreTwo
        state.currentTurn = snap.turn
        state.stateSequence = snap.sequence
        for (i in state.balls.indices) {
            if (i >= snap.ballX.size) break
            val b = state.balls[i]
            b.x = snap.ballX[i]
            b.y = snap.ballY[i]
            b.potted = snap.potted[i]
            b.stop()
        }
        state.lastUpdateMillis = System.currentTimeMillis()
    }
}

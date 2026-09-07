package com.university.poolseclab.security

import com.university.poolseclab.game.GameState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds the list of inspectable variables by reading the application own
 * [GameState]. Nothing outside this process is touched.
 */
object StateInspector {

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun variables(state: GameState): List<StateVariable> {
        val list = ArrayList<StateVariable>(32)

        list.add(
            StateVariable(
                name = "playerId",
                path = "GameState -> Player -> Id",
                type = "String",
                value = state.playerId,
                address = SimulatedAddresses.field(SimulatedAddresses.PLAYER_STATE, 0x00),
                description = "Identifier of the local player for this laboratory session.",
                risk = "An identifier that the client can rewrite lets one account impersonate another when the server trusts whatever identifier arrives with a request.",
                defense = "Derive the identity on the server from an authenticated session token. Never accept an identity that the client simply asserts."
            )
        )

        list.add(
            StateVariable(
                name = "matchId",
                path = "GameState -> Match -> Id",
                type = "String",
                value = state.matchId,
                address = SimulatedAddresses.field(SimulatedAddresses.MATCH_STATE, 0x00),
                description = "Identifier of the match currently being played.",
                risk = "If a result can be submitted for any match identifier, an attacker can report outcomes for matches they never played, or replay one winning result many times.",
                defense = "Server issued, single use match identifiers bound to the authenticated session, with a server side record of which matches are still open."
            )
        )

        list.add(
            StateVariable(
                name = "currentTurn",
                path = "GameState -> Match -> CurrentTurn",
                type = "Int (1 or 2)",
                value = state.currentTurn.toString(),
                address = SimulatedAddresses.field(SimulatedAddresses.MATCH_STATE, 0x08),
                description = "Which player is allowed to take the next shot.",
                risk = "A client that decides whose turn it is can simply keep the turn forever and take unlimited consecutive shots.",
                defense = "The authoritative turn lives on the server. The client renders a turn, it does not grant one."
            )
        )

        list.add(
            StateVariable(
                name = "playerPosition",
                path = "GameState -> Player -> Position",
                type = "Int (seat index)",
                value = "seat " + state.currentTurn + " of 2",
                address = SimulatedAddresses.field(SimulatedAddresses.PLAYER_STATE, 0x04),
                description = "Seat that the local player occupies in the match.",
                risk = "Seat confusion can be used to act on behalf of the opponent if the server maps actions to seats supplied by the client.",
                defense = "Map every incoming action to a seat using the server side session, not a field in the request body."
            )
        )

        val cue = state.cueBall
        list.add(
            StateVariable(
                name = "cueBall.position",
                path = "GameState -> CueBall -> Position",
                type = "Vector2f (x, y)",
                value = String.format(Locale.US, "(%.2f, %.2f)", cue.x, cue.y),
                address = SimulatedAddresses.field(SimulatedAddresses.CUE_BALL_STATE, 0x00),
                description = "Position of the cue ball on the 200 by 100 unit table.",
                risk = "Writing this value directly is a teleport. The ball reaches a position that no legal shot could have produced.",
                defense = "Recompute positions from the shot on the authoritative side, and validate that every reported position is reachable from the previous one within the elapsed time."
            )
        )

        list.add(
            StateVariable(
                name = "cueBall.velocity",
                path = "GameState -> CueBall -> Velocity",
                type = "Vector2f (vx, vy)",
                value = String.format(Locale.US, "(%.2f, %.2f)", cue.vx, cue.vy),
                address = SimulatedAddresses.field(SimulatedAddresses.CUE_BALL_STATE, 0x08),
                description = "Current velocity of the cue ball in table units per second.",
                risk = "A velocity above the physical maximum of the simulation is an impossible value. It is both a cheat and, usefully, an easy signal to detect.",
                defense = "Range check every physical quantity against the limits the simulation itself can produce, and reject anything outside them."
            )
        )

        for (i in 0 until 4) {
            val b = state.balls.getOrNull(i + 1) ?: continue
            list.add(
                StateVariable(
                    name = "balls[" + i + "].position",
                    path = "GameState -> Balls[" + i + "] -> Position",
                    type = "Vector2f (x, y)",
                    value = String.format(Locale.US, "(%.2f, %.2f)", b.x, b.y),
                    address = SimulatedAddresses.field(SimulatedAddresses.ballBase(i), 0x00),
                    description = "Position of object ball number " + b.number + ".",
                    risk = "Object balls arranged into a winning layout give an instant, unearned victory.",
                    defense = "Treat the whole table layout as one signed unit. Verify the signature before the layout is used to decide anything."
                )
            )
            list.add(
                StateVariable(
                    name = "balls[" + i + "].velocity",
                    path = "GameState -> Balls[" + i + "] -> Velocity",
                    type = "Vector2f (vx, vy)",
                    value = String.format(Locale.US, "(%.2f, %.2f)", b.vx, b.vy),
                    address = SimulatedAddresses.field(SimulatedAddresses.ballBase(i), 0x08),
                    description = "Velocity of object ball number " + b.number + ".",
                    risk = "Injected velocity moves balls without a shot, which breaks the link between input and outcome.",
                    defense = "Derive motion only from an accepted shot. Never accept motion as an input in its own right."
                )
            )
        }

        list.add(
            StateVariable(
                name = "shotPower",
                path = "GameState -> Shot -> Power",
                type = "Float (0.0 .. 1.0)",
                value = String.format(Locale.US, "%.2f", state.shotPower),
                address = SimulatedAddresses.field(SimulatedAddresses.MATCH_STATE, 0x10),
                description = "Normalised power of the last shot.",
                risk = "A power outside the normalised range produces motion the game was never designed to allow.",
                defense = "Clamp on receipt as well as on send. An input validated only by the sender is not validated."
            )
        )

        list.add(
            StateVariable(
                name = "shotAngle",
                path = "GameState -> Shot -> AngleDeg",
                type = "Float (degrees)",
                value = String.format(Locale.US, "%.1f", state.shotAngleDeg),
                address = SimulatedAddresses.field(SimulatedAddresses.MATCH_STATE, 0x14),
                description = "Direction of the last shot in degrees.",
                risk = "Angle by itself is harmless, but with power it is the whole input to the simulation, so it is the value an automated aiming tool would drive.",
                defense = "Send the shot input and let the authoritative simulation produce the outcome, rather than sending the outcome."
            )
        )

        list.add(
            StateVariable(
                name = "remainingBalls",
                path = "GameState -> Match -> RemainingBalls",
                type = "Int",
                value = state.balls.count { !it.potted && !it.isCue }.toString(),
                address = SimulatedAddresses.field(SimulatedAddresses.MATCH_STATE, 0x18),
                description = "How many object balls are still on the table.",
                risk = "A derived counter that is stored rather than recomputed can drift out of agreement with the array it summarises, and disagreement is exactly what an attacker exploits.",
                defense = "Recompute derived values from the underlying data on the authoritative side instead of trusting a stored counter."
            )
        )

        list.add(
            StateVariable(
                name = "playerScore",
                path = "GameState -> Player -> Score",
                type = "Int (0 .. 7)",
                value = state.playerOneScore.toString() + " / opponent " + state.playerTwoScore,
                address = SimulatedAddresses.field(SimulatedAddresses.PLAYER_STATE, 0x08),
                description = "Number of balls of the player own group that have been potted.",
                risk = "Score written straight to 7 is an instant win in any design where the client reports the score.",
                defense = "Score is a consequence of accepted shots. Recompute it, do not receive it."
            )
        )

        list.add(
            StateVariable(
                name = "virtualBalance",
                path = "GameState -> Player -> Balance",
                type = "Long (coins)",
                value = state.virtualBalance.toString(),
                address = SimulatedAddresses.field(SimulatedAddresses.PLAYER_STATE, 0x10),
                description = "Simulated in-app currency held for this laboratory session only. It buys nothing and has no value.",
                risk = "This is the classic target. If the client is the only place the balance lives, then whoever controls the device controls the economy of the game.",
                defense = "Keep the authoritative balance on the server, change it only through server side transactions, and treat the client copy as a display cache."
            )
        )

        list.add(
            StateVariable(
                name = "matchState",
                path = "GameState -> Match -> Phase",
                type = "Enum",
                value = state.matchPhase.name,
                address = SimulatedAddresses.field(SimulatedAddresses.MATCH_STATE, 0x1C),
                description = "Whether the match is ready, in motion, or finished.",
                risk = "Forcing the phase straight to a win state skips every rule between the break and the result.",
                defense = "Allow only legal transitions, and enforce that state machine where the attacker cannot reach it."
            )
        )

        list.add(
            StateVariable(
                name = "gameState.sequence",
                path = "GameState -> Sequence",
                type = "Long (monotonic)",
                value = state.stateSequence.toString(),
                address = SimulatedAddresses.field(SimulatedAddresses.GAME_STATE, 0x08),
                description = "Counter incremented by every sanctioned change to the state.",
                risk = "If a counter can move backwards, an old but validly signed state can be replayed, and a signature alone will not notice.",
                defense = "Require the counter to increase strictly, and bind it into the signed payload so a rewind invalidates the signature."
            )
        )

        list.add(
            StateVariable(
                name = "timestamp",
                path = "GameState -> LastUpdate",
                type = "Long (epoch millis)",
                value = state.lastUpdateMillis.toString() + "  (" + timeFormat.format(Date(state.lastUpdateMillis)) + ")",
                address = SimulatedAddresses.field(SimulatedAddresses.GAME_STATE, 0x10),
                description = "When the state was last modified, taken from the device clock.",
                risk = "The device clock belongs to the device owner. Any expiry, cooldown or rate limit that depends on it can be moved at will.",
                defense = "Use server time for anything security relevant. Treat client timestamps as a hint for the user interface only."
            )
        )

        return list
    }
}

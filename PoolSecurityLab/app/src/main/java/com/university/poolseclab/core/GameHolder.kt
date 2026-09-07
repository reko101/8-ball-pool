package com.university.poolseclab.core

import com.university.poolseclab.game.PoolGame
import com.university.poolseclab.security.IntegrityGuard

/**
 * Single shared instance of the match and of the guard that watches it.
 *
 * Every screen reads from this one object, so the values in the Security Lab
 * are the same values the table is being drawn from. There is no copy and no
 * second source of truth.
 */
object GameHolder {

    val game: PoolGame = PoolGame()
    val guard: IntegrityGuard = IntegrityGuard(game.state)

    private var initialised = false

    @Synchronized
    fun ensureInitialised() {
        if (initialised) return
        guard.seal()
        initialised = true
    }
}

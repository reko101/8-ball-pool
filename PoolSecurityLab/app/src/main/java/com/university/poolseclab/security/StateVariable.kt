package com.university.poolseclab.security

/**
 * One inspectable value, together with the teaching material that belongs to it.
 *
 * [path] uses the logical notation asked for in the assignment, for example
 * "GameState -> Player -> Balance". [address] is a simulated address only.
 */
data class StateVariable(
    val name: String,
    val path: String,
    val type: String,
    val value: String,
    val address: String,
    val description: String,
    val risk: String,
    val defense: String
)

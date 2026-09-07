package com.university.poolseclab.security

import java.util.Locale

/**
 * Invented addresses used only for drawing a picture of a memory layout.
 *
 * IMPORTANT
 * =========
 * Every number in this file was chosen by the author of this teaching app.
 * They are not real addresses, not offsets, not pointers and not signatures.
 * They do not correspond to any commercial application, and nothing in this
 * app ever reads or writes memory outside its own objects.
 *
 * The point of the exercise is the SHAPE of a state layout: a struct per
 * concept, a fixed stride per array element, and predictable field offsets.
 * That shape is what makes client held state easy to locate, and that is the
 * lesson, not the specific numbers.
 */
object SimulatedAddresses {

    const val GAME_STATE = 0x1000L
    const val PLAYER_STATE = 0x1040L
    const val MATCH_STATE = 0x1080L
    const val CUE_BALL_STATE = 0x10C0L
    const val BALL_STATE_BASE = 0x1100L
    const val BALL_STRIDE = 0x40L

    fun hex(address: Long): String =
        String.format(Locale.US, "0x%04X", address)

    fun field(base: Long, offset: Long): String =
        String.format(Locale.US, "0x%04X  (base 0x%04X + 0x%02X)", base + offset, base, offset)

    fun ballBase(index: Int): Long = BALL_STATE_BASE + index * BALL_STRIDE
}

package com.university.poolseclab.security

/**
 * The defensive techniques this build demonstrates, and an honest statement of
 * how far each one goes when it runs on hardware the player controls.
 */
object DefenseCatalog {

    data class Technique(
        val name: String,
        val whatItDoes: String,
        val scope: String
    )

    val techniques: List<Technique> = listOf(
        Technique(
            "Canonical serialisation",
            "Turns the protected part of the state into one deterministic string so that the same state always produces the same bytes.",
            "Fully implemented in this build"
        ),
        Technique(
            "Hashing and HMAC-SHA256",
            "Computes a keyed tag over the canonical state and compares it against the tag recorded when the state was last sealed.",
            "Computed for real here, but only meaningful when the key lives on a server"
        ),
        Technique(
            "State validation",
            "Checks that the turn is a valid seat, that the phase is a legal enum value and that derived counters agree with the arrays they summarise.",
            "Fully implemented in this build"
        ),
        Technique(
            "Range checking",
            "Rejects a balance outside 0 to 100000 and a score outside 0 to 7.",
            "Fully implemented in this build"
        ),
        Technique(
            "Impossible value detection",
            "Rejects a balance increase larger than any single match can pay, a ball outside the playing surface, and a speed above the maximum the simulation can produce.",
            "Fully implemented in this build"
        ),
        Technique(
            "Replay and rewind detection",
            "Requires a monotonic sequence counter, so that an older but validly signed state cannot be presented again.",
            "Fully implemented in this build"
        ),
        Technique(
            "Server authoritative design",
            "The server simulates the shot, owns the balance and the score, and sends the client a result to display rather than accepting one.",
            "Explained only. It cannot be demonstrated offline, and that limitation is itself the lesson"
        ),
        Technique(
            "Secure storage principles",
            "Secrets belong in the Android Keystore where the key material is not extractable, sensitive state is not written to shared storage, and backup is disabled. This build persists nothing at all.",
            "Principles explained. This build has nothing to store"
        ),
        Technique(
            "Shrinking and obfuscation",
            "R8 is enabled for the release build. It raises the cost of reading the code, which slows an analyst down.",
            "Enabled, but it is a speed bump and not a security control"
        )
    )

    fun asText(): String {
        val sb = StringBuilder()
        for ((i, t) in techniques.withIndex()) {
            sb.append(i + 1).append(". ").append(t.name).append('\n')
            sb.append("   ").append(t.whatItDoes).append('\n')
            sb.append("   Scope: ").append(t.scope).append('\n')
            if (i != techniques.lastIndex) sb.append('\n')
        }
        return sb.toString()
    }
}

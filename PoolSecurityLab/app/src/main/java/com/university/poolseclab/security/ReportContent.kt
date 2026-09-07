package com.university.poolseclab.security

/** One titled section of the in-app security report. */
data class ReportSection(val title: String, val body: String)

/**
 * The written material for the Security Report screen. It is kept in Kotlin
 * rather than in strings.xml because it is long form prose rather than a set of
 * user interface labels.
 */
object ReportContent {

    val sections: List<ReportSection> = listOf(

        ReportSection(
            "0. Scope of this laboratory",
            "This application is a self contained teaching bench. It contains its own pool " +
                    "simulation, its own state and its own defences.\n\n" +
                    "It does not read, write, patch, hook, overlay or communicate with any other " +
                    "application. It declares no permissions at all, including no INTERNET " +
                    "permission. Every address shown on the memory screen was invented for the " +
                    "diagram and corresponds to nothing outside this app.\n\n" +
                    "The exercise is about understanding a class of weakness and the controls " +
                    "that answer it. It is not about attacking a product."
        ),

        ReportSection(
            "1. What game-state manipulation means",
            "A game holds its state in the memory of the process that runs it: where the balls " +
                    "are, whose turn it is, what the score is, how much virtual currency the player " +
                    "has. Game-state manipulation is changing one of those values outside the rules " +
                    "that were supposed to govern it.\n\n" +
                    "The important part is the word outside. A player who pots a ball changes the " +
                    "score, and that is fine, because the change travelled through the code path " +
                    "that is allowed to make it. Manipulation is a change that arrives at the value " +
                    "by another route, so none of the conditions the rules would have enforced were " +
                    "ever tested.\n\n" +
                    "This is why an integrity check that only asks did this value change is close to " +
                    "useless. The question worth asking is: did this change arrive through a path " +
                    "that was permitted to make it."
        ),

        ReportSection(
            "2. Why client-side-only values are vulnerable",
            "A value that exists only on the player device sits on hardware the player owns, " +
                    "administers and can inspect at leisure. There is no time limit, no observer and " +
                    "no cost to failure.\n\n" +
                    "Three properties make client held state easy to find, and this app deliberately " +
                    "has all three:\n\n" +
                    "  Structure. State is grouped into records, so finding one field tends to " +
                    "reveal its neighbours.\n\n" +
                    "  Regularity. Arrays use a fixed stride, so element zero gives you every other " +
                    "element by arithmetic.\n\n" +
                    "  Observability. A value the interface displays can be located by watching what " +
                    "changes when the display changes.\n\n" +
                    "None of these is a bug. They are ordinary, sensible engineering. The mistake is " +
                    "not that the state is structured, it is trusting a structured value that lives " +
                    "somewhere you do not control."
        ),

        ReportSection(
            "3. Client-authoritative versus server-authoritative",
            "In a client-authoritative design the device decides what happened and tells the " +
                    "server the result: I won, my score is seven, my balance is now nine thousand. " +
                    "The server records it. Every value in that report is exactly as trustworthy as " +
                    "the device that produced it, which is to say not at all.\n\n" +
                    "In a server-authoritative design the device sends inputs, not outcomes: I shot " +
                    "at 41 degrees with 62 percent power. The server runs the same simulation, " +
                    "produces the outcome itself, and sends back the state to display. The client " +
                    "becomes a renderer and an input device.\n\n" +
                    "The shift matters because of where the attacker sits. Tampering with a " +
                    "client-authoritative game changes the answer. Tampering with a " +
                    "server-authoritative game changes only what one player sees on their own " +
                    "screen, while the match itself proceeds from the server copy.\n\n" +
                    "The honest engineering caveat is cost. Server simulation costs computation, " +
                    "bandwidth and latency, and single player or casual modes are often left " +
                    "client-authoritative on purpose. That is a defensible decision as long as it is " +
                    "a decision, and as long as nothing of value crosses from that mode into a " +
                    "shared economy."
        ),

        ReportSection(
            "4. Why virtual currency must never be trusted from the client",
            "Currency is the highest value target because it is fungible, it usually persists " +
                    "across sessions, and it often has a real money purchase path beside it. A " +
                    "balance that the client can set is a mint.\n\n" +
                    "The rule is that the authoritative balance lives on the server and changes only " +
                    "through server side transactions that are themselves validated: this player " +
                    "won this match, this match paid this reward, this reward has not already been " +
                    "claimed. The number the client holds is a display cache. If it disagrees with " +
                    "the server, the server is right by definition.\n\n" +
                    "Note what this app does in its rules layer. Every change to the balance goes " +
                    "through one method, awardCoinsSanctioned. Funnelling mutation through a single " +
                    "checkable path does not stop an attacker who can write the field directly, but " +
                    "it is what makes a server side version of the same design enforceable, and it " +
                    "is what lets the tamper screen tell a legitimate change from an injected one."
        ),

        ReportSection(
            "5. How memory tampering works, conceptually",
            "Conceptually the attack has three phases, and it is worth understanding them at " +
                    "this level because the defences map onto them.\n\n" +
                    "Locate. The attacker finds the value, usually by changing something visible and " +
                    "narrowing down what changed with it.\n\n" +
                    "Understand. The attacker works out the layout around it: the type, the record " +
                    "it belongs to, the stride of the array it sits in.\n\n" +
                    "Modify or replay. The attacker writes a new value, or captures a valid state " +
                    "and presents it again later.\n\n" +
                    "The corresponding defences are: make the value not worth locating by keeping " +
                    "the authoritative copy elsewhere; make an incorrect value detectable by binding " +
                    "the state together cryptographically and validating it against the rules of the " +
                    "simulation; and make replay useless by binding a monotonic counter into the " +
                    "signed payload.\n\n" +
                    "Deliberately absent from that list: any technique for performing the attack. " +
                    "The defensive design does not require it, and this laboratory does not teach it."
        ),

        ReportSection(
            "6. How anti-cheat systems detect abnormal behaviour",
            "Detection is mostly statistics rather than cryptography, because a cheat has to " +
                    "produce results, and results leave a distribution.\n\n" +
                    "Impossible values. A speed above the maximum the simulation can generate, a " +
                    "position off the table, a balance that grew faster than any sequence of matches " +
                    "could pay. This app implements all three; they are cheap and they catch the " +
                    "unsubtle case.\n\n" +
                    "Impossible transitions. A ball that moved further than the elapsed time allows, " +
                    "or a phase that jumped from break to victory without the states in between.\n\n" +
                    "Behavioural outliers. Aiming error that is too small too consistently, reaction " +
                    "times below human capability, or a win rate that no honest player sustains. " +
                    "Humans are noisy; automation usually is not.\n\n" +
                    "Cross-account correlation. The same anomaly appearing across accounts that " +
                    "share a device, a payment method or a session pattern.\n\n" +
                    "Two engineering cautions. First, every detector has a false positive rate, and " +
                    "a skilled player looks statistically strange, so detection should feed a review " +
                    "process rather than an instant ban. Second, detection is a control on the " +
                    "server. A detector that runs on the attacker device is subject to the same " +
                    "problem as the value it is watching."
        ),

        ReportSection(
            "7. Defensive programming techniques",
            "Validate on the trust boundary, not before it. Clamping in the sender is user " +
                    "interface work. The check that counts is the one on the side that does not " +
                    "trust the sender.\n\n" +
                    "Make state transitions explicit. An enum plus a table of legal transitions " +
                    "turns whole classes of manipulation into an unreachable state rather than a " +
                    "condition somebody has to remember to test.\n\n" +
                    "Funnel mutation. One sanctioned method per protected value gives you a single " +
                    "place to check, audit and log.\n\n" +
                    "Recompute derived values. Anything derivable from the base data should be " +
                    "recomputed and not received, so no disagreement can be manufactured.\n\n" +
                    "Bind data together. Signing fields individually lets an attacker mix and match " +
                    "valid pieces. Sign the whole state as one canonical payload, with the identity, " +
                    "the match and the sequence counter inside it.\n\n" +
                    "Bind time and order. A monotonic counter inside the signed payload turns replay " +
                    "into a signature failure.\n\n" +
                    "Fail closed and quietly. Reject the state and resend the authoritative copy. Do " +
                    "not print which check failed; a precise error message is free tuning feedback " +
                    "for the attacker.\n\n" +
                    "Keep secrets out of the client. A key in the application binary is not a " +
                    "secret. On Android, keys that must exist on the device belong in the Keystore, " +
                    "where the material is not extractable, and they should authorise a request " +
                    "rather than certify a claim.\n\n" +
                    "Be honest about obfuscation. R8 is enabled in the release build of this project " +
                    "and it makes static reading harder. It raises cost. It does not create a trust " +
                    "boundary, and a design that depends on it has no boundary at all."
        ),

        ReportSection(
            "8. What this build demonstrates and what it cannot",
            "Genuinely implemented and running: canonical serialisation, HMAC-SHA256 over the " +
                    "protected state, range checks, geometry and physics plausibility checks, " +
                    "monotonic sequence checking, sanctioned mutation paths, and restoration of a " +
                    "trusted baseline.\n\n" +
                    "Explained but not demonstrable offline: server-authoritative simulation, server " +
                    "side transaction validation, server issued match identifiers, server time, and " +
                    "cross-account correlation. All of these need a party the player does not " +
                    "control, and this application deliberately has no network access.\n\n" +
                    "The most important conclusion of the exercise is the one the tamper screen " +
                    "states about itself: the verifier runs in the same process as the value it " +
                    "verifies, and holds its key there too. A client cannot be made to enforce rules " +
                    "against the person who owns it. Client side integrity checking raises cost and " +
                    "produces useful telemetry. It is not a trust boundary, and it should never be " +
                    "the only thing standing between a player and the economy of a game."
        )
    )
}

# Pool Security Lab — concepts and viva preparation

## Part A — the cybersecurity concepts this project demonstrates

**1. Trust boundaries.** The core idea of the whole project. A trust boundary is a
line across which you stop believing what you are told. In a mobile game the boundary
is the device: everything on the player's side of it is under the player's control.
The project makes this concrete by putting a real HMAC verifier *inside* the app and
then showing that the verifier's own key sits in the same process as the value it
protects.

**2. Client-authoritative vs server-authoritative design.** A client-authoritative
client reports *outcomes* ("I won, my balance is 9000"). A server-authoritative client
sends *inputs* ("41 degrees, 62% power") and renders whatever the server computes.
The project's `PoolGame` is a complete, deterministic simulation — exactly the kind of
code that would run on the server in an authoritative design.

**3. Canonical serialisation.** `IntegrityGuard.canonicalState()` produces one
deterministic string for a given state. Without canonicalisation a signature over
structured data is meaningless, because the same state could serialise two ways.

**4. Keyed integrity (HMAC-SHA256).** A tag over the canonical payload detects any
modification of protected fields. The project computes this for real via `javax.crypto.Mac`.

**5. Validation categories.** Four distinct kinds of check, all implemented:
*range* (balance 0–100000, score 0–7), *domain* (turn ∈ {1,2}), *geometry* (ball inside
the 200×100 surface), *physics* (speed ≤ simulation maximum).

**6. Impossible-value / impossible-delta detection.** Absolute range is weaker than
rate. `balance − sealedBalance > maxBalanceGainPerMatch` catches a gain no match could pay,
even if the resulting number is individually plausible.

**7. Replay and rollback detection.** A monotonic `stateSequence` bound into the signed
payload. Rewinding it is caught; and because the counter is inside the HMAC, replaying an
old validly-signed state also fails.

**8. Sanctioned mutation paths.** Every legitimate balance change goes through
`awardCoinsSanctioned()`. This is what lets the tamper screen distinguish *a value changed*
from *a value changed through a path that was not allowed to change it*.

**9. Fail-closed recovery.** `RESTORE TRUSTED STATE` overwrites the modified fields with
the sealed baseline — modelling a server that does not argue with a client, it just resends
authoritative state.

**10. Defence in depth, and its limits.** R8 shrinking/obfuscation is enabled in the release
build and is explicitly described as a cost-raiser, not a control. Attack-surface reduction
is demonstrated by declaring zero permissions.

---

## Part B — likely examiner questions, with answers

**Q1. Explain your project in two sentences.**
It is a self-contained offline pool game that doubles as a laboratory for client-side state
integrity. It exposes its own game state, lets you tamper with that state inside the app's own
sandbox, detects the tampering with real cryptographic and rule-based checks, and then explains
honestly why those checks are not sufficient on their own.

**Q2. Does this interact with any commercial game?**
No. It declares zero permissions — including no `INTERNET` — and never reads, writes, hooks,
patches or overlays another application. Every value it inspects is a field of its own
`GameState` object. The memory addresses on screen were invented for the diagram.

**Q3. Then why show a memory map at all, if the addresses are fake?**
Because the lesson is the *shape*, not the numbers: state grouped into records, arrays at a
fixed stride, and values that are observable because the UI displays them. Those three
properties are what make client-held state locatable, and they are properties of good ordinary
engineering — not bugs. The specific addresses are irrelevant to the lesson.

**Q4. Your HMAC key is in the app. Isn't that a vulnerability?**
It is the finding, not an oversight. The app says so on the tamper screen and in section 8 of
the report. An attacker who can rewrite `virtualBalance` can equally read the key or patch out
the verifier. That demonstrates the actual principle: *a client cannot enforce rules against
the person who owns it.* The same construction becomes a real control only when the key and
the check live on a server.

**Q5. So is client-side integrity checking worthless?**
No — but it must be understood as economics, not security. It raises attacker cost, filters
low-effort tampering, and generates telemetry that server-side detection can act on. What it
cannot do is create a trust boundary. It should never be the only thing between a player and
the game's economy.

**Q6. Walk me through what happens when I press "Simulate balance overwrite".**
The handler assigns `state.virtualBalance = 999_999` directly, deliberately bypassing
`awardCoinsSanctioned()`, so `stateSequence` does not advance. On `RUN INTEGRITY CHECK`,
`canonicalState()` is recomputed and its HMAC no longer matches the sealed tag, so the
per-field diff reports Balance 1000 → 999999. Two independent rules also fire: the range check
(> 100000) and the impossible-gain check (+998999 against a per-match maximum of 100). Three
independent detections of one modification — that redundancy is intentional.

**Q7. Why does awarding 25 coins *not* trigger a violation, when it also changes the balance?**
Because it went through the sanctioned path, which advanced the sequence counter and re-sealed
the baseline. This is the single most important distinction in the project: integrity checking
does not ask *did this change*, it asks *did this change arrive through a permitted path*.

**Q8. How would you make this genuinely secure?**
Move the simulation server-side. The client sends `(angle, power)` for its own turn; the server
runs the same deterministic `PhysicsEngine`, owns the balance, the score and the turn, and
returns state to render. Match IDs are server-issued and single-use, rewards are idempotent
transactions, and all timing uses server time. The client keeps the local checks as a cheap
first filter and a telemetry source.

**Q9. Why is your physics engine deterministic, and why does that matter?**
Fixed 1/480 s substeps and no randomness mean identical input produces identical output. That
matters for two reasons: a server can re-simulate a client's shot and compare, and the lab
itself is reproducible, so a detection result can be demonstrated repeatedly to an examiner.

**Q10. Why `minSdk 26`?**
Adaptive launcher icons require API 26, and this project ships vector icons rather than
bitmap density buckets. API 26+ also covers essentially the whole active device base.

**Q11. Anything specific to Android 15 (API 35)?**
Yes. Apps targeting SDK 35 are laid out edge-to-edge by default, so `MainActivity` applies
`systemBars() | displayCutout()` insets as padding; without that the UI would sit under the
status bar and gesture bar. Backup and data-extraction rules are also declared, both excluding
everything, since the app persists nothing.

**Q12. How does anti-cheat detect cheaters in real systems?**
Mostly statistically, server-side: impossible values, impossible transitions (movement further
than elapsed time allows), behavioural outliers (aim error too small too consistently,
superhuman reaction times), and cross-account correlation. Two cautions: every detector has a
false-positive rate, so detection should feed review rather than instant bans; and a detector
running on the attacker's device inherits the same problem as the value it watches.

**Q13. What is the weakest part of your own project?**
Three things. The rule set is simplified 8-ball (no ball-in-hand placement, no called shots).
The physics ignores spin, cushion angle and ball mass differences. And there is no server, so
the most important control in the whole report — server authority — is described rather than
demonstrated. I would add a small local HTTP authority as the next iteration to close that gap.

**Q14. What did you learn?**
That most "cheat detection" questions are really design questions asked too late. Once a value
of consequence lives only on the player's device, no amount of hashing, obfuscation or checking
recovers the situation — the fix is to move the authority, and the client-side work is a cost
multiplier layered on top of that, never a substitute for it.

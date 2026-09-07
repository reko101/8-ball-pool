package com.university.poolseclab.security

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.university.poolseclab.core.GameHolder
import com.university.poolseclab.databinding.FragmentTamperBinding
import com.university.poolseclab.game.TableSpec

/**
 * The tamper demonstration.
 *
 * Every button below writes to a field of THIS application, bypassing the
 * sanctioned setters in PoolGame. That models what an attacker with access to
 * the process would do, without touching anything outside the app sandbox.
 */
class TamperLabFragment : Fragment() {

    private var binding: FragmentTamperBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val b = FragmentTamperBinding.inflate(inflater, container, false)
        binding = b
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = binding ?: return
        val state = GameHolder.game.state
        val guard = GameHolder.guard

        b.defensesText.text = DefenseCatalog.asText()

        b.sealBtn.setOnClickListener {
            guard.seal()
            b.statusBanner.setTextColor(Color.parseColor("#4FC3F7"))
            b.statusBanner.text = "BASELINE SEALED"
            b.reportText.text = buildString {
                append("Trusted baseline recorded at ").append(guard.sealedAtText).append('\n')
                append("Sequence   : ").append(state.stateSequence).append('\n')
                append("Balance    : ").append(state.virtualBalance).append('\n')
                append("Score      : ").append(state.playerOneScore).append(" - ").append(state.playerTwoScore).append('\n')
                append("HMAC tag   : ").append(guard.currentTag().take(32)).append("...\n\n")
                append("Now apply a simulated modification, then run the integrity check.")
            }
        }

        b.verifyBtn.setOnClickListener { renderReport(guard.verify()) }

        b.restoreBtn.setOnClickListener {
            guard.restore()
            b.statusBanner.setTextColor(Color.parseColor("#4FC3F7"))
            b.statusBanner.text = "TRUSTED STATE RESTORED"
            b.reportText.text =
                "The sealed baseline was written back over the modified fields.\n\n" +
                        "This is what a server authoritative game does after it rejects a client " +
                        "report: it does not argue with the client, it simply resends the state " +
                        "that it, and not the client, considers true.\n\n" +
                        "Balance  : " + state.virtualBalance + "\n" +
                        "Score    : " + state.playerOneScore + " - " + state.playerTwoScore + "\n" +
                        "Sequence : " + state.stateSequence
        }

        // ---- Simulated unauthorised modifications --------------------
        b.tBalanceBtn.setOnClickListener {
            val before = state.virtualBalance
            state.virtualBalance = 999_999L      // direct write, no sanctioned setter
            announce("Balance overwritten directly: " + before + " -> " + state.virtualBalance)
        }

        b.tScoreBtn.setOnClickListener {
            val before = state.playerOneScore
            state.playerOneScore = 99
            announce("Score overwritten directly: " + before + " -> " + state.playerOneScore)
        }

        b.tTeleportBtn.setOnClickListener {
            val cue = state.cueBall
            val bx = cue.x
            val by = cue.y
            cue.x = -40f
            cue.y = -25f
            announce(
                "Cue ball teleported off the table: (" +
                        String.format("%.1f", bx) + ", " + String.format("%.1f", by) +
                        ") -> (-40.0, -25.0)"
            )
        }

        b.tSpeedBtn.setOnClickListener {
            val cue = state.cueBall
            cue.vx = TableSpec.MAX_SPEED * 12f
            cue.vy = 0f
            announce(
                "Impossible velocity injected: vx = " +
                        String.format("%.0f", cue.vx) +
                        ", simulation maximum is " + String.format("%.0f", TableSpec.MAX_SPEED)
            )
        }

        b.tReplayBtn.setOnClickListener {
            val before = state.stateSequence
            state.stateSequence = if (before >= 5L) before - 5L else 0L
            announce("Sequence counter rewound: " + before + " -> " + state.stateSequence)
        }

        // ---- Control experiment -------------------------------------
        b.tLegitBtn.setOnClickListener {
            GameHolder.game.awardCoinsSanctioned(25L)
            GameHolder.guard.seal()
            b.statusBanner.setTextColor(Color.parseColor("#2E7D32"))
            b.statusBanner.text = "SANCTIONED CHANGE APPLIED"
            b.reportText.text =
                "25 coins were added through awardCoinsSanctioned(), the only method the " +
                        "rules layer allows to change the balance. The sequence counter advanced and " +
                        "the baseline was re-sealed.\n\n" +
                        "Run the integrity check now: it reports no violation.\n\n" +
                        "That is the distinction the whole exercise turns on. Integrity checking " +
                        "does not ask whether a value changed. It asks whether the change came " +
                        "through a path that was allowed to make it.\n\n" +
                        "Balance  : " + state.virtualBalance + "\n" +
                        "Sequence : " + state.stateSequence
        }
    }

    private fun announce(what: String) {
        val b = binding ?: return
        b.statusBanner.setTextColor(Color.parseColor("#F9A825"))
        b.statusBanner.text = "MODIFICATION APPLIED - NOT YET CHECKED"
        b.reportText.text = what + "\n\nNow press RUN INTEGRITY CHECK."
    }

    private fun renderReport(report: IntegrityReport) {
        val b = binding ?: return

        if (report.neverSealed) {
            b.statusBanner.setTextColor(Color.parseColor("#F9A825"))
            b.statusBanner.text = "NO BASELINE"
            b.reportText.text = "Press SEAL BASELINE first. There is nothing to compare against yet."
            return
        }

        val sb = StringBuilder()
        sb.append("Detection time : ").append(report.detectedAt).append('\n')
        sb.append("Sealed at      : ").append(GameHolder.guard.sealedAtText).append('\n')
        sb.append("Expected tag   : ").append(report.expectedTag.take(32)).append("...\n")
        sb.append("Actual tag     : ").append(report.actualTag.take(32)).append("...\n")
        sb.append("Sequence       : sealed ").append(report.sealedSequence)
            .append(", current ").append(report.currentSequence).append('\n')
        sb.append('\n')

        if (report.ok) {
            b.statusBanner.setTextColor(Color.parseColor("#2E7D32"))
            b.statusBanner.text = "INTEGRITY: OK"
            sb.append("Integrity status : OK\n")
            sb.append("No unexpected change was found. Every protected field matches the sealed baseline and every range, geometry, physics and replay check passed.")
        } else {
            b.statusBanner.setTextColor(Color.parseColor("#C62828"))
            b.statusBanner.text = "WARNING: GAME-STATE INTEGRITY VIOLATION DETECTED"
            sb.append("Integrity status : VIOLATION\n")
            sb.append("Findings         : ").append(report.findings.size).append("\n\n")
            for ((i, f) in report.findings.withIndex()) {
                sb.append(i + 1).append(") ").append(f.field).append('\n')
                sb.append("    original value : ").append(f.originalValue).append('\n')
                sb.append("    current value  : ").append(f.currentValue).append('\n')
                sb.append("    triggered rule : ").append(f.rule).append('\n')
                if (i != report.findings.lastIndex) sb.append('\n')
            }
            sb.append("\n\nHonest note: this check runs in the same process as the value it is ")
            sb.append("checking, so an attacker who can change the balance can also change this ")
            sb.append("verifier or read its key. The check is a speed bump on the client and a ")
            sb.append("real control only when it runs on a server.")
        }

        b.reportText.text = sb.toString()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}

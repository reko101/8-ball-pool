package com.university.poolseclab.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.university.poolseclab.core.GameHolder
import com.university.poolseclab.databinding.FragmentGameBinding
import java.util.Locale

/** The playable table. */
class GameFragment : Fragment() {

    private var binding: FragmentGameBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val b = FragmentGameBinding.inflate(inflater, container, false)
        binding = b
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = binding ?: return
        val game = GameHolder.game

        b.poolView.attach(game)

        b.poolView.onAimChanged = { _ ->
            binding?.let { bb -> updateHeader(bb) }
        }

        b.poolView.onShotFinished = {
            val message = game.onBallsStopped()
            // Every legitimate change to the state re-seals the integrity baseline.
            GameHolder.guard.seal()
            binding?.let {
                it.statusText.text = message
                updateHeader(it)
                it.poolView.invalidate()
            }
        }

        b.powerBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                game.state.shotPower = progress / 100f
                binding?.powerValue?.text = String.format(Locale.US, "%d%%", progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        b.powerBar.progress = (game.state.shotPower * 100f).toInt().coerceIn(5, 100)

        b.shootBtn.setOnClickListener {
            if (!game.canShoot()) return@setOnClickListener
            game.shoot(b.powerBar.progress / 100f, game.state.shotAngleDeg)
            b.poolView.kick()
            updateHeader(b)
        }

        b.resetBtn.setOnClickListener {
            game.newGame()
            GameHolder.guard.seal()
            b.statusText.text = game.state.statusMessage
            b.poolView.invalidate()
            updateHeader(b)
        }

        b.statusText.text = game.state.statusMessage
        updateHeader(b)
    }

    override fun onResume() {
        super.onResume()
        binding?.let {
            updateHeader(it)
            it.poolView.invalidate()
        }
    }

    private fun updateHeader(b: FragmentGameBinding) {
        val s = GameHolder.game.state
        val groupText = when (s.groupOf(s.currentTurn)) {
            BallGroup.SOLIDS -> "solids"
            BallGroup.STRIPES -> "stripes"
            else -> "open table"
        }
        val header = when (s.matchPhase) {
            MatchPhase.PLAYER_ONE_WINS -> "Player 1 wins"
            MatchPhase.PLAYER_TWO_WINS -> "Player 2 wins"
            else -> String.format(
                Locale.US,
                "Player %d  (%s)   %d - %d   angle %.0f deg   coins %d",
                s.currentTurn, groupText, s.playerOneScore, s.playerTwoScore,
                s.shotAngleDeg, s.virtualBalance
            )
        }
        b.turnText.text = header
    }

    override fun onDestroyView() {
        binding?.poolView?.onShotFinished = null
        binding?.poolView?.onAimChanged = null
        binding = null
        super.onDestroyView()
    }
}

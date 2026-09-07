package com.university.poolseclab.security

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.university.poolseclab.R
import com.university.poolseclab.core.GameHolder
import com.university.poolseclab.databinding.DialogVariableBinding
import com.university.poolseclab.databinding.FragmentMemoryBinding
import com.university.poolseclab.databinding.ItemMemoryFieldBinding
import com.university.poolseclab.databinding.ItemMemoryHeaderBinding

/** One row of the simulated memory picture. */
sealed class MemoryRow {
    data class Header(val line: String, val note: String) : MemoryRow()
    data class Field(val variable: StateVariable) : MemoryRow()
}

/**
 * Draws an invented struct layout and lets the student open a teaching card for
 * any field. Nothing here reads real memory.
 */
class MemoryMapFragment : Fragment() {

    private var binding: FragmentMemoryBinding? = null
    private lateinit var adapter: MemoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val b = FragmentMemoryBinding.inflate(inflater, container, false)
        binding = b
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = binding ?: return
        adapter = MemoryAdapter { showCard(it) }
        b.list.layoutManager = LinearLayoutManager(requireContext())
        b.list.adapter = adapter
        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (this::adapter.isInitialized) refresh()
    }

    private fun refresh() {
        adapter.submit(buildRows())
    }

    private fun buildRows(): List<MemoryRow> {
        val vars = StateInspector.variables(GameHolder.game.state)
        val rows = ArrayList<MemoryRow>()

        rows.add(
            MemoryRow.Header(
                SimulatedAddresses.hex(SimulatedAddresses.GAME_STATE) + "  GameState        (root, 0x40 bytes)",
                "Simulated. The root record that owns every other record below."
            )
        )
        rows.addAll(vars.filter { it.path.startsWith("GameState -> Sequence") || it.path.startsWith("GameState -> LastUpdate") }.map { MemoryRow.Field(it) })

        rows.add(
            MemoryRow.Header(
                SimulatedAddresses.hex(SimulatedAddresses.PLAYER_STATE) + "  PlayerState      (0x40 bytes)",
                "Simulated. Identity, score and the simulated currency."
            )
        )
        rows.addAll(vars.filter { it.path.startsWith("GameState -> Player") }.map { MemoryRow.Field(it) })

        rows.add(
            MemoryRow.Header(
                SimulatedAddresses.hex(SimulatedAddresses.MATCH_STATE) + "  MatchState       (0x40 bytes)",
                "Simulated. Turn, phase and the parameters of the last shot."
            )
        )
        rows.addAll(vars.filter { it.path.startsWith("GameState -> Match") || it.path.startsWith("GameState -> Shot") }.map { MemoryRow.Field(it) })

        rows.add(
            MemoryRow.Header(
                SimulatedAddresses.hex(SimulatedAddresses.CUE_BALL_STATE) + "  CueBallState     (0x40 bytes)",
                "Simulated. Position and velocity of the cue ball."
            )
        )
        rows.addAll(vars.filter { it.path.startsWith("GameState -> CueBall") }.map { MemoryRow.Field(it) })

        val ballVars = vars.filter { it.path.startsWith("GameState -> Balls[") }
        for (i in 0 until 4) {
            val prefix = "GameState -> Balls[" + i + "]"
            val group = ballVars.filter { it.path.startsWith(prefix) }
            if (group.isEmpty()) continue
            rows.add(
                MemoryRow.Header(
                    SimulatedAddresses.hex(SimulatedAddresses.ballBase(i)) + "  BallState[" + i + "]     (stride 0x40)",
                    "Simulated. A fixed stride array is what makes element " + i + " trivially reachable once element 0 is known."
                )
            )
            rows.addAll(group.map { MemoryRow.Field(it) })
        }

        rows.add(
            MemoryRow.Header(
                SimulatedAddresses.hex(SimulatedAddresses.ballBase(4)) + " .. " +
                        SimulatedAddresses.hex(SimulatedAddresses.ballBase(14)) + "  BallState[4..14]",
                "Simulated. The remaining object balls follow the same 0x40 stride."
            )
        )

        return rows
    }

    private fun showCard(v: StateVariable) {
        val ctx = context ?: return
        val db = DialogVariableBinding.inflate(LayoutInflater.from(ctx))
        db.dName.text = v.name + "\n" + v.path
        db.dType.text = v.type
        db.dValue.text = v.value
        db.dAddress.text = v.address + "\n(simulated, not a real address)"
        db.dDesc.text = v.description
        db.dRisk.text = v.risk
        db.dDefense.text = v.defense

        AlertDialog.Builder(ctx)
            .setTitle(v.name)
            .setView(db.root)
            .setPositiveButton(R.string.dlg_close, null)
            .show()
    }

    override fun onDestroyView() {
        binding?.list?.adapter = null
        binding = null
        super.onDestroyView()
    }
}

/** Two view type adapter: struct headers and clickable fields. */
class MemoryAdapter(
    private val onFieldClick: (StateVariable) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val rows = ArrayList<MemoryRow>()

    fun submit(newRows: List<MemoryRow>) {
        rows.clear()
        rows.addAll(newRows)
        notifyDataSetChanged()
    }

    class HeaderHolder(val binding: ItemMemoryHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    class FieldHolder(val binding: ItemMemoryFieldBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int =
        if (rows[position] is MemoryRow.Header) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) {
            HeaderHolder(ItemMemoryHeaderBinding.inflate(inflater, parent, false))
        } else {
            FieldHolder(ItemMemoryFieldBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is MemoryRow.Header -> {
                val h = holder as HeaderHolder
                h.binding.hdrLine.text = row.line
                h.binding.hdrNote.text = row.note
            }
            is MemoryRow.Field -> {
                val f = holder as FieldHolder
                val v = row.variable
                f.binding.fieldLine.text = v.address.substringBefore("  ") + "   " + v.name
                f.binding.fieldMeta.text = v.type + " = " + v.value
                f.binding.root.setOnClickListener { onFieldClick(v) }
            }
        }
    }

    override fun getItemCount(): Int = rows.size
}

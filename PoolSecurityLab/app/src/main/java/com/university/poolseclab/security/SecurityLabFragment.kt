package com.university.poolseclab.security

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.university.poolseclab.core.GameHolder
import com.university.poolseclab.databinding.FragmentStateBinding
import com.university.poolseclab.databinding.ItemStateVarBinding

/** Lists every inspectable value of the application own state. */
class SecurityLabFragment : Fragment() {

    private var binding: FragmentStateBinding? = null
    private val adapter = StateVarAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val b = FragmentStateBinding.inflate(inflater, container, false)
        binding = b
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = binding ?: return
        b.list.layoutManager = LinearLayoutManager(requireContext())
        b.list.adapter = adapter
        b.refreshBtn.setOnClickListener { refresh() }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        adapter.submit(StateInspector.variables(GameHolder.game.state))
    }

    override fun onDestroyView() {
        binding?.list?.adapter = null
        binding = null
        super.onDestroyView()
    }
}

/** Simple adapter over [StateVariable]. */
class StateVarAdapter : RecyclerView.Adapter<StateVarAdapter.Holder>() {

    private val items = ArrayList<StateVariable>()

    fun submit(newItems: List<StateVariable>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemStateVarBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)
        return Holder(ItemStateVarBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val v = items[position]
        holder.binding.varName.text = v.name
        holder.binding.varPath.text = v.path
        holder.binding.varType.text = v.type
        holder.binding.varAddress.text = v.address
        holder.binding.varValue.text = v.value
    }

    override fun getItemCount(): Int = items.size
}

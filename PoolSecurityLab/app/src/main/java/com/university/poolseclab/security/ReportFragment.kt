package com.university.poolseclab.security

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.university.poolseclab.databinding.FragmentReportBinding
import com.university.poolseclab.databinding.ItemReportBinding

/** The written security report. */
class ReportFragment : Fragment() {

    private var binding: FragmentReportBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val b = FragmentReportBinding.inflate(inflater, container, false)
        binding = b
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = binding ?: return
        b.list.layoutManager = LinearLayoutManager(requireContext())
        b.list.adapter = ReportAdapter(ReportContent.sections)
    }

    override fun onDestroyView() {
        binding?.list?.adapter = null
        binding = null
        super.onDestroyView()
    }
}

class ReportAdapter(
    private val sections: List<ReportSection>
) : RecyclerView.Adapter<ReportAdapter.Holder>() {

    class Holder(val binding: ItemReportBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)
        return Holder(ItemReportBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val s = sections[position]
        holder.binding.secTitle.text = s.title
        holder.binding.secBody.text = s.body
    }

    override fun getItemCount(): Int = sections.size
}

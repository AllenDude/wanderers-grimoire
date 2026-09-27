package com.allen.wanderersgrimoire

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FolderAdapter(
    private val getFolders: () -> List<Pair<String, String>>, // (id, label), "all" included
    private val getActive: () -> String,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<FolderAdapter.VH>() {

    class VH(val text: TextView) : RecyclerView.ViewHolder(text)

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_folder, parent, false) as TextView
        return VH(view)
    }

    override fun getItemCount() = getFolders().size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (id, label) = getFolders()[position]
        holder.text.text = label
        val active = id == getActive()
        holder.text.setBackgroundResource(
            if (active) R.drawable.chip_background_active else R.drawable.chip_background
        )
        holder.text.setOnClickListener { onClick(id) }
    }
}

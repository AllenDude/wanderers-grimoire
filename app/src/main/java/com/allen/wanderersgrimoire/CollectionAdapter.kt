package com.allen.wanderersgrimoire

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// "all" is a synthetic row representing every saved item, always shown
// first, not a real Folder.
data class CollectionRow(
    val id: String,
    val name: String,
    val icon: String,
    val count: Int,
    val deletable: Boolean
)

class CollectionAdapter(
    private val getRows: () -> List<CollectionRow>,
    private val onClick: (String) -> Unit,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<CollectionAdapter.VH>() {

    class VH(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val icon: TextView = view.findViewById(R.id.collectionIcon)
        val name: TextView = view.findViewById(R.id.collectionName)
        val count: TextView = view.findViewById(R.id.collectionCount)
        val delete: TextView = view.findViewById(R.id.collectionDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.collection_row, parent, false)
        return VH(view)
    }

    override fun getItemCount() = getRows().size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = getRows()[position]
        holder.icon.text = row.icon
        holder.name.text = row.name
        holder.count.text = row.count.toString()
        holder.delete.visibility = if (row.deletable) android.view.View.VISIBLE else android.view.View.GONE
        holder.itemView.setOnClickListener { onClick(row.id) }
        holder.delete.setOnClickListener { onDelete(row.id) }
    }
}

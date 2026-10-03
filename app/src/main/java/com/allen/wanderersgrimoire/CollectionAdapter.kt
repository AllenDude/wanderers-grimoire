package com.allen.wanderersgrimoire

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// "all" is a synthetic row representing every saved item, always shown
// first, not a real Folder.
data class CollectionRow(
    val id: String,
    val name: String,
    val count: Int,
    val deletable: Boolean
)

class CollectionAdapter(
    private val theme: Theme,
    private val getRows: () -> List<CollectionRow>,
    private val onClick: (String) -> Unit,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<CollectionAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.collectionIcon)
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

        holder.itemView.background = ThemeManager.cardDrawable(holder.itemView.context, theme, theme.surface)
        holder.icon.setImageResource(R.drawable.ic_collection)
        holder.icon.setColorFilter(theme.accent)
        holder.name.text = row.name
        holder.name.setTextColor(theme.text)
        holder.count.text = row.count.toString()
        holder.count.setTextColor(theme.textDim)
        holder.delete.setTextColor(theme.danger)
        holder.delete.visibility = if (row.deletable) View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener { onClick(row.id) }
        holder.delete.setOnClickListener { onDelete(row.id) }
    }
}

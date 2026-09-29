package com.allen.wanderersgrimoire

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

// A feed row is either a day-group header or an actual item, so headers can
// be interleaved with cards in a single scrolling list.
sealed class FeedRow {
    data class Header(val label: String) : FeedRow()
    data class Row(val item: Item) : FeedRow()
}

class FeedAdapter(
    private val getRows: () -> List<FeedRow>,
    private val getFolderName: (String?) -> String?,
    private val onToggleDone: (Item) -> Unit,
    private val onTogglePinned: (Item) -> Unit,
    private val onEdit: (Item) -> Unit,
    private val onDelete: (Item) -> Unit,
    private val onOpenLink: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        val label: TextView = view as TextView
    }

    class ItemVH(view: View) : RecyclerView.ViewHolder(view) {
        val strip: View = view.findViewById(R.id.typeStrip)
        val check: CheckBox = view.findViewById(R.id.taskCheck)
        val title: TextView = view.findViewById(R.id.itemTitle)
        val star: TextView = view.findViewById(R.id.starBtn)
        val image: ImageView = view.findViewById(R.id.itemImage)
        val subtitle: TextView = view.findViewById(R.id.itemSubtitle)
        val body: TextView = view.findViewById(R.id.itemBody)
        val folderTag: TextView = view.findViewById(R.id.itemFolderTag)
        val time: TextView = view.findViewById(R.id.itemTime)
        val edit: TextView = view.findViewById(R.id.editBtn)
        val delete: TextView = view.findViewById(R.id.deleteBtn)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getRows()[position]) {
            is FeedRow.Header -> TYPE_HEADER
            is FeedRow.Row -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.header_date, parent, false)
            HeaderVH(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_row, parent, false)
            ItemVH(view)
        }
    }

    override fun getItemCount() = getRows().size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getRows()[position]) {
            is FeedRow.Header -> bindHeader(holder as HeaderVH, row.label)
            is FeedRow.Row -> bindItem(holder as ItemVH, row.item)
        }
    }

    private fun bindHeader(holder: HeaderVH, label: String) {
        holder.label.text = label
    }

    private fun bindItem(holder: ItemVH, item: Item) {
        val ctx = holder.itemView.context

        val colorRes = when (item.type) {
            "link" -> R.color.link
            "task" -> R.color.task
            else -> R.color.note
        }
        holder.strip.setBackgroundColor(ContextCompat.getColor(ctx, colorRes))

        val typeIcon = when (item.type) {
            "link" -> "🔗"
            "task" -> "✅"
            else -> "📝"
        }
        holder.title.text = "$typeIcon ${item.title}"

        if (item.type == "task") {
            holder.check.visibility = View.VISIBLE
            holder.check.setOnCheckedChangeListener(null)
            holder.check.isChecked = item.done
            holder.check.setOnCheckedChangeListener { _, _ -> onToggleDone(item) }
        } else {
            holder.check.visibility = View.GONE
        }

        holder.star.text = if (item.pinned) "★" else "☆"
        holder.star.setOnClickListener { onTogglePinned(item) }

        val imagePath = item.imagePath
        if (imagePath != null) {
            holder.image.visibility = View.VISIBLE
            holder.image.setImageBitmap(null)
            val bmp = ImageStore.loadSampled(ctx, imagePath, 400, 400)
            if (bmp != null) holder.image.setImageBitmap(bmp)
        } else {
            holder.image.visibility = View.GONE
            holder.image.setImageBitmap(null)
        }

        if (item.type == "link" && item.url.isNotBlank()) {
            holder.subtitle.visibility = View.VISIBLE
            holder.subtitle.text = "🌐 " + FeedUtils.domainOf(item.url)
            holder.subtitle.setOnClickListener { onOpenLink(item.url) }
        } else {
            holder.subtitle.visibility = View.GONE
            holder.subtitle.setOnClickListener(null)
        }

        if (item.body.isNotBlank()) {
            holder.body.visibility = View.VISIBLE
            holder.body.text = item.body
        } else {
            holder.body.visibility = View.GONE
        }

        val folderName = getFolderName(item.folderId)
        if (folderName != null) {
            holder.folderTag.visibility = View.VISIBLE
            holder.folderTag.text = folderName
        } else {
            holder.folderTag.visibility = View.GONE
        }

        holder.time.text = FeedUtils.timeLabel(item.createdAt)

        holder.edit.setOnClickListener { onEdit(item) }
        holder.delete.setOnClickListener { onDelete(item) }
    }
}

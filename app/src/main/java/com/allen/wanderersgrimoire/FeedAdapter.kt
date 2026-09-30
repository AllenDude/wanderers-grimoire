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
        private const val TYPE_IMAGE_TILE = 2
    }

    class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        val label: TextView = view as TextView
    }

    class ItemVH(view: View) : RecyclerView.ViewHolder(view) {
        val strip: View = view.findViewById(R.id.typeStrip)
        val check: CheckBox = view.findViewById(R.id.taskCheck)
        val title: TextView = view.findViewById(R.id.itemTitle)
        val star: TextView = view.findViewById(R.id.starBtn)
        val subtitle: TextView = view.findViewById(R.id.itemSubtitle)
        val body: TextView = view.findViewById(R.id.itemBody)
        val folderTag: TextView = view.findViewById(R.id.itemFolderTag)
        val time: TextView = view.findViewById(R.id.itemTime)
        val edit: TextView = view.findViewById(R.id.editBtn)
        val delete: TextView = view.findViewById(R.id.deleteBtn)
    }

    class TileVH(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.tileImage)
        val star: TextView = view.findViewById(R.id.tileStar)
        val caption: TextView = view.findViewById(R.id.tileCaption)
    }

    // Headers and text-only cards take the full row width; anything with an
    // attached image becomes a half-width tile so images fall into a
    // 2-column grid instead of one huge card per photo.
    fun spanSizeAt(position: Int): Int {
        return when (val row = getRows()[position]) {
            is FeedRow.Header -> 2
            is FeedRow.Row -> if (row.item.imagePath != null) 1 else 2
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val row = getRows()[position]) {
            is FeedRow.Header -> TYPE_HEADER
            is FeedRow.Row -> if (row.item.imagePath != null) TYPE_IMAGE_TILE else TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.header_date, parent, false)
                HeaderVH(view)
            }
            TYPE_IMAGE_TILE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image_tile, parent, false)
                TileVH(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_row, parent, false)
                ItemVH(view)
            }
        }
    }

    override fun getItemCount() = getRows().size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getRows()[position]) {
            is FeedRow.Header -> bindHeader(holder as HeaderVH, row.label)
            is FeedRow.Row -> {
                if (row.item.imagePath != null) {
                    bindTile(holder as TileVH, row.item)
                } else {
                    bindItem(holder as ItemVH, row.item)
                }
            }
        }
    }

    private fun bindHeader(holder: HeaderVH, label: String) {
        holder.label.text = label
    }

    private fun bindTile(holder: TileVH, item: Item) {
        val ctx = holder.itemView.context
        val path = item.imagePath

        holder.image.setImageBitmap(null)
        if (path != null) {
            val bmp = ImageStore.loadSampled(ctx, path, 400, 400)
            if (bmp != null) holder.image.setImageBitmap(bmp)
        }

        holder.star.text = if (item.pinned) "★" else "☆"
        holder.star.setOnClickListener { onTogglePinned(item) }

        holder.caption.text = item.title
        holder.itemView.setOnClickListener { onEdit(item) }
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

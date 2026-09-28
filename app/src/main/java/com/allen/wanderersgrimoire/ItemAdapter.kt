package com.allen.wanderersgrimoire

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ItemAdapter(
    private val getItems: () -> List<Item>,
    private val getFolderName: (String?) -> String?,
    private val onToggleDone: (Item) -> Unit,
    private val onEdit: (Item) -> Unit,
    private val onDelete: (Item) -> Unit,
    private val onOpenLink: (String) -> Unit
) : RecyclerView.Adapter<ItemAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val strip: View = view.findViewById(R.id.typeStrip)
        val check: CheckBox = view.findViewById(R.id.taskCheck)
        val title: TextView = view.findViewById(R.id.itemTitle)
        val image: ImageView = view.findViewById(R.id.itemImage)
        val body: TextView = view.findViewById(R.id.itemBody)
        val folderTag: TextView = view.findViewById(R.id.itemFolderTag)
        val edit: TextView = view.findViewById(R.id.editBtn)
        val delete: TextView = view.findViewById(R.id.deleteBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_row, parent, false)
        return VH(view)
    }

    override fun getItemCount() = getItems().size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItems()[position]
        val ctx = holder.itemView.context

        val colorRes = when (item.type) {
            "pin" -> R.color.pin
            "task" -> R.color.task
            else -> R.color.note
        }
        holder.strip.setBackgroundColor(ContextCompat.getColor(ctx, colorRes))

        holder.title.text = item.title

        if (item.type == "task") {
            holder.check.visibility = View.VISIBLE
            holder.check.setOnCheckedChangeListener(null)
            holder.check.isChecked = item.done
            holder.check.setOnCheckedChangeListener { _, _ -> onToggleDone(item) }
        } else {
            holder.check.visibility = View.GONE
        }

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

        val bodyText = if (item.type == "pin" && item.url.isNotBlank()) {
            item.url + if (item.body.isNotBlank()) "\n" + item.body else ""
        } else {
            item.body
        }

        if (bodyText.isNotBlank()) {
            holder.body.visibility = View.VISIBLE
            holder.body.text = bodyText
            holder.body.setOnClickListener {
                if (item.type == "pin" && item.url.isNotBlank()) onOpenLink(item.url)
            }
        } else {
            holder.body.visibility = View.GONE
            holder.body.setOnClickListener(null)
        }

        val folderName = getFolderName(item.folderId)
        if (folderName != null) {
            holder.folderTag.visibility = View.VISIBLE
            holder.folderTag.text = folderName
        } else {
            holder.folderTag.visibility = View.GONE
        }

        holder.edit.setOnClickListener { onEdit(item) }
        holder.delete.setOnClickListener { onDelete(item) }
    }
}


package com.allen.wanderersgrimoire

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.UUID

/** Fields to pre-fill the add dialog with, used when content arrives via
 *  Android's share sheet from another app instead of being typed in. */
data class Prefill(
    val title: String = "",
    val body: String = "",
    val url: String = "",
    val imagePath: String? = null
)

class MainActivity : AppCompatActivity() {

    private lateinit var storage: Storage
    private var activeFolder: String = "all"
    private var query: String = ""

    private lateinit var folderAdapter: FolderAdapter
    private lateinit var itemAdapter: ItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        storage = Storage(this)
        storage.load()

        val folderList = findViewById<RecyclerView>(R.id.folderList)
        folderList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        folderAdapter = FolderAdapter(
            getFolders = { listOf("all" to "All") + storage.folders.map { it.id to it.name } },
            getActive = { activeFolder },
            onClick = { id -> activeFolder = id; refreshList() }
        )
        folderList.adapter = folderAdapter

        val itemList = findViewById<RecyclerView>(R.id.itemList)
        itemList.layoutManager = LinearLayoutManager(this)
        itemAdapter = ItemAdapter(
            getItems = { visibleItems() },
            getFolderName = { id -> storage.folders.find { it.id == id }?.name },
            onToggleDone = { item -> item.done = !item.done; storage.save(); refreshList() },
            onEdit = { item -> openDialog(item.type, item) },
            onDelete = { item ->
                item.imagePath?.let { ImageStore.delete(this, it) }
                storage.items.remove(item)
                storage.save()
                refreshList()
            },
            onOpenLink = { url ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (_: Exception) {
                }
            }
        )
        itemList.adapter = itemAdapter

        findViewById<Button>(R.id.addNoteBtn).setOnClickListener { openDialog("note", null) }
        findViewById<Button>(R.id.addPinBtn).setOnClickListener { openDialog("pin", null) }
        findViewById<Button>(R.id.addTaskBtn).setOnClickListener { openDialog("task", null) }
        findViewById<Button>(R.id.newFolderBtn).setOnClickListener { newFolderDialog() }

        findViewById<EditText>(R.id.searchInput).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                query = s.toString().trim().lowercase()
                refreshList()
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        refreshList()
        handleShareIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    /** Catches anything shared in from another app: Instagram/TikTok/FB
     *  links share as plain text, a screenshot or saved image shares as
     *  image/*. Either way we pre-fill the add dialog rather than saving
     *  silently, so there's a chance to pick a folder before it's filed. */
    private fun handleShareIntent(intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_SEND) return

        val mimeType = intent.type ?: ""

        if (mimeType.startsWith("image/")) {
            val uri = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            if (uri != null) {
                val savedPath = ImageStore.saveFromUri(this, uri)
                if (savedPath != null) {
                    openDialog("note", null, Prefill(imagePath = savedPath))
                }
            }
        } else {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim() ?: return
            val matcher = Patterns.WEB_URL.matcher(sharedText)
            if (matcher.find()) {
                val foundUrl = matcher.group()
                val leftover = sharedText.replace(foundUrl, "").trim()
                openDialog("pin", null, Prefill(title = leftover, url = foundUrl))
            } else {
                openDialog("note", null, Prefill(body = sharedText))
            }
        }

        // Consumed, don't re-fire this same shared content if the
        // activity gets recreated (e.g. on rotation).
        intent.action = null
    }

    private fun visibleItems(): List<Item> {
        var list = storage.items.sortedByDescending { it.createdAt }
        if (activeFolder != "all") list = list.filter { it.folderId == activeFolder }
        if (query.isNotBlank()) {
            list = list.filter { (it.title + " " + it.body).lowercase().contains(query) }
        }
        return list
    }

    private fun refreshList() {
        folderAdapter.notifyDataSetChanged()
        itemAdapter.notifyDataSetChanged()
        findViewById<TextView>(R.id.emptyView).visibility =
            if (visibleItems().isEmpty()) View.VISIBLE else View.GONE
    }

    private fun newFolderDialog() {
        val input = EditText(this)
        input.hint = "Folder name"
        AlertDialog.Builder(this)
            .setTitle("New folder")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotBlank()) {
                    storage.folders.add(Folder(UUID.randomUUID().toString(), name))
                    storage.save()
                    refreshList()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openDialog(type: String, existing: Item?, prefill: Prefill? = null) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_item, null)
        val imageField = view.findViewById<ImageView>(R.id.dialogImage)
        val titleField = view.findViewById<EditText>(R.id.dialogTitle)
        val urlField = view.findViewById<EditText>(R.id.dialogUrl)
        val bodyField = view.findViewById<EditText>(R.id.dialogBody)
        val folderSpinner = view.findViewById<Spinner>(R.id.dialogFolderSpinner)

        urlField.visibility = if (type == "pin") View.VISIBLE else View.GONE

        val folderOptions = listOf("No folder") + storage.folders.map { it.name }
        folderSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, folderOptions)

        // imagePath for this save: comes from whichever item we're editing,
        // or from a freshly shared-in image, never both.
        val attachedImagePath = existing?.imagePath ?: prefill?.imagePath

        if (existing != null) {
            titleField.setText(existing.title)
            urlField.setText(existing.url)
            bodyField.setText(existing.body)
            val idx = storage.folders.indexOfFirst { it.id == existing.folderId }
            folderSpinner.setSelection(if (idx >= 0) idx + 1 else 0)
        } else {
            titleField.setText(prefill?.title ?: "")
            urlField.setText(prefill?.url ?: "")
            bodyField.setText(prefill?.body ?: "")
            val idx = storage.folders.indexOfFirst { it.id == activeFolder }
            folderSpinner.setSelection(if (idx >= 0) idx + 1 else 0)
        }

        if (attachedImagePath != null) {
            val bmp = ImageStore.loadSampled(this, attachedImagePath, 600, 600)
            if (bmp != null) {
                imageField.setImageBitmap(bmp)
                imageField.visibility = View.VISIBLE
            }
        }

        AlertDialog.Builder(this)
            .setTitle(if (existing != null) "Edit" else "Add $type")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val title = titleField.text.toString().trim().ifBlank { "Untitled" }
                val body = bodyField.text.toString().trim()
                val url = urlField.text.toString().trim()
                val spinnerPos = folderSpinner.selectedItemPosition
                val folderId = if (spinnerPos == 0) null else storage.folders[spinnerPos - 1].id

                if (existing != null) {
                    existing.title = title
                    existing.body = body
                    existing.url = url
                    existing.folderId = folderId
                } else {
                    storage.items.add(
                        0,
                        Item(
                            id = UUID.randomUUID().toString(),
                            type = type,
                            title = title,
                            body = body,
                            url = url,
                            folderId = folderId,
                            imagePath = attachedImagePath
                        )
                    )
                }
                storage.save()
                refreshList()
            }
            .setNegativeButton("Cancel") { _, _ ->
                // A shared-in image that gets cancelled shouldn't linger as
                // an orphaned file with nothing pointing to it.
                if (existing == null && prefill?.imagePath != null) {
                    ImageStore.delete(this, prefill.imagePath)
                }
            }
            .show()
    }
}

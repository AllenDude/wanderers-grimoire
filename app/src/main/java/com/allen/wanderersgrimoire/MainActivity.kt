package com.allen.wanderersgrimoire

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.UUID

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

    private fun openDialog(type: String, existing: Item?) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_item, null)
        val titleField = view.findViewById<EditText>(R.id.dialogTitle)
        val urlField = view.findViewById<EditText>(R.id.dialogUrl)
        val bodyField = view.findViewById<EditText>(R.id.dialogBody)
        val folderSpinner = view.findViewById<Spinner>(R.id.dialogFolderSpinner)

        urlField.visibility = if (type == "pin") View.VISIBLE else View.GONE

        val folderOptions = listOf("No folder") + storage.folders.map { it.name }
        folderSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, folderOptions)

        if (existing != null) {
            titleField.setText(existing.title)
            urlField.setText(existing.url)
            bodyField.setText(existing.body)
            val idx = storage.folders.indexOfFirst { it.id == existing.folderId }
            folderSpinner.setSelection(if (idx >= 0) idx + 1 else 0)
        } else {
            val idx = storage.folders.indexOfFirst { it.id == activeFolder }
            folderSpinner.setSelection(if (idx >= 0) idx + 1 else 0)
        }

        AlertDialog.Builder(this)
            .setTitle(if (existing != null) "Edit" else "Add $type")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val title = titleField.text.toString().trim()
                if (title.isBlank()) return@setPositiveButton
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
                            folderId = folderId
                        )
                    )
                }
                storage.save()
                refreshList()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

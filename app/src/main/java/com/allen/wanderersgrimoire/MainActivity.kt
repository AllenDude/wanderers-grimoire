package com.allen.wanderersgrimoire

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.Fade
import android.transition.TransitionManager
import android.util.Patterns
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.UUID

// Fields to pre-fill the add dialog with, used when content arrives via
// Android's share sheet or the in-app image picker instead of being typed.
data class Prefill(
    val title: String = "",
    val body: String = "",
    val url: String = "",
    val imagePath: String? = null
)

enum class Screen { HOME, COLLECTIONS, PINS, SETTINGS }

class MainActivity : AppCompatActivity() {

    private lateinit var storage: Storage

    private var activeScreen: Screen = Screen.HOME
    private var activeType: String = "all" // all, note, link, task, image, pinned
    private var activeFolder: String = "all"
    private var query: String = ""
    private var sortAscending: Boolean = false

    // Tracks whether each empty state was already showing, so the fade-in
    // only plays on the gone-to-visible transition, not on every refresh.
    private var emptyViewShowing = false
    private var pinsEmptyViewShowing = false

    private lateinit var feedAdapter: FeedAdapter
    private lateinit var pinsAdapter: FeedAdapter
    private lateinit var collectionAdapter: CollectionAdapter

    // Views
    private lateinit var screenContainer: ViewGroup
    private lateinit var homeScreen: View
    private lateinit var collectionsScreen: View
    private lateinit var pinsScreen: View
    private lateinit var settingsScreen: View
    private lateinit var headerStats: TextView
    private lateinit var sectionTitleRow: ViewGroup
    private lateinit var sectionTitle: TextView
    private lateinit var seeAllBtn: TextView
    private lateinit var emptyView: View
    private lateinit var pinsEmptyView: View
    private lateinit var activeFolderBanner: View
    private lateinit var activeFolderLabel: TextView
    private lateinit var chipAll: TextView
    private lateinit var chipNotes: TextView
    private lateinit var chipLinks: TextView
    private lateinit var chipTasks: TextView
    private lateinit var chipImages: TextView
    private lateinit var chipPins: TextView
    private lateinit var navHome: TextView
    private lateinit var navCollections: TextView
    private lateinit var navPins: TextView
    private lateinit var navSettings: TextView
    private lateinit var feedList: RecyclerView
    private lateinit var pinsList: RecyclerView
    private lateinit var collectionsList: RecyclerView

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val savedPath = ImageStore.saveFromUri(this, uri)
            if (savedPath != null) {
                openDialog("note", null, Prefill(imagePath = savedPath))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        storage = Storage(this)
        storage.load()

        bindViews()
        setupFeed()
        setupPins()
        setupCollections()
        setupChips()
        setupNav()
        setupSearch()
        setupSettings()

        showScreen(Screen.HOME)
        handleShareIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun bindViews() {
        screenContainer = findViewById(R.id.screenContainer)
        homeScreen = findViewById(R.id.homeScreen)
        collectionsScreen = findViewById(R.id.collectionsScreen)
        pinsScreen = findViewById(R.id.pinsScreen)
        settingsScreen = findViewById(R.id.settingsScreen)
        headerStats = findViewById(R.id.headerStats)
        sectionTitleRow = findViewById(R.id.sectionTitleRow)
        sectionTitle = findViewById(R.id.sectionTitle)
        seeAllBtn = findViewById(R.id.seeAllBtn)
        emptyView = findViewById(R.id.emptyView)
        pinsEmptyView = findViewById(R.id.pinsEmptyView)
        activeFolderBanner = findViewById(R.id.activeFolderBanner)
        activeFolderLabel = findViewById(R.id.activeFolderLabel)
        chipAll = findViewById(R.id.chipAll)
        chipNotes = findViewById(R.id.chipNotes)
        chipLinks = findViewById(R.id.chipLinks)
        chipTasks = findViewById(R.id.chipTasks)
        chipImages = findViewById(R.id.chipImages)
        chipPins = findViewById(R.id.chipPins)
        navHome = findViewById(R.id.navHome)
        navCollections = findViewById(R.id.navCollections)
        navPins = findViewById(R.id.navPins)
        navSettings = findViewById(R.id.navSettings)
        feedList = findViewById(R.id.feedList)
        pinsList = findViewById(R.id.pinsList)
        collectionsList = findViewById(R.id.collectionsList)
    }

    private fun setupFeed() {
        feedAdapter = FeedAdapter(
            getRows = { buildFeedRows() },
            getFolderName = { id -> storage.folders.find { it.id == id }?.name },
            onToggleDone = { item -> item.done = !item.done; storage.save(); refreshAll() },
            onTogglePinned = { item -> item.pinned = !item.pinned; storage.save(); refreshAll() },
            onEdit = { item -> openDialog(item.type, item) },
            onDelete = { item -> deleteItem(item) },
            onOpenLink = { url -> openLink(url) }
        )
        val gridManager = GridLayoutManager(this, 2)
        gridManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int = feedAdapter.spanSizeAt(position)
        }
        feedList.layoutManager = gridManager
        feedList.adapter = feedAdapter

        findViewById<TextView>(R.id.clearFolderFilter).setOnClickListener {
            activeFolder = "all"
            refreshAll()
        }

        findViewById<TextView>(R.id.sortToggle).setOnClickListener {
            sortAscending = !sortAscending
            it.alpha = if (sortAscending) 1f else 0.6f
            refreshAll()
        }

        seeAllBtn.setOnClickListener {
            activeType = "all"
            activeFolder = "all"
            query = ""
            findViewById<EditText>(R.id.searchInput).setText("")
            updateChipStyles()
            refreshAll()
        }

        findViewById<TextView>(R.id.emptyAddBtn).setOnClickListener { openQuickCapture() }
    }

    private fun setupPins() {
        pinsAdapter = FeedAdapter(
            getRows = { storage.items.filter { it.pinned }.sortedByDescending { it.createdAt }.map { FeedRow.Row(it) } },
            getFolderName = { id -> storage.folders.find { it.id == id }?.name },
            onToggleDone = { item -> item.done = !item.done; storage.save(); refreshAll() },
            onTogglePinned = { item -> item.pinned = !item.pinned; storage.save(); refreshAll() },
            onEdit = { item -> openDialog(item.type, item) },
            onDelete = { item -> deleteItem(item) },
            onOpenLink = { url -> openLink(url) }
        )
        val gridManager = GridLayoutManager(this, 2)
        gridManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int = pinsAdapter.spanSizeAt(position)
        }
        pinsList.layoutManager = gridManager
        pinsList.adapter = pinsAdapter
    }

    private fun setupCollections() {
        collectionsList.layoutManager = LinearLayoutManager(this)
        collectionAdapter = CollectionAdapter(
            getRows = { buildCollectionRows() },
            onClick = { id ->
                activeFolder = id
                activeType = "all"
                updateChipStyles()
                showScreen(Screen.HOME)
            },
            onDelete = { id -> deleteFolder(id) }
        )
        collectionsList.adapter = collectionAdapter

        findViewById<android.widget.Button>(R.id.newCollectionBtn).setOnClickListener { newFolderDialog() }
    }

    private fun setupChips() {
        chipAll.setOnClickListener { activeType = "all"; updateChipStyles(); refreshAll() }
        chipNotes.setOnClickListener { activeType = "note"; updateChipStyles(); refreshAll() }
        chipLinks.setOnClickListener { activeType = "link"; updateChipStyles(); refreshAll() }
        chipTasks.setOnClickListener { activeType = "task"; updateChipStyles(); refreshAll() }
        chipImages.setOnClickListener { activeType = "image"; updateChipStyles(); refreshAll() }
        chipPins.setOnClickListener { activeType = "pinned"; updateChipStyles(); refreshAll() }
        updateChipStyles()
    }

    private fun setupNav() {
        navHome.setOnClickListener { showScreen(Screen.HOME) }
        navCollections.setOnClickListener { showScreen(Screen.COLLECTIONS) }
        navPins.setOnClickListener { showScreen(Screen.PINS) }
        navSettings.setOnClickListener { showScreen(Screen.SETTINGS) }

        val captureBtn = findViewById<TextView>(R.id.navCapture)
        captureBtn.setOnClickListener { openQuickCapture() }
        // A small press-in squish, like a real Material FAB: shrinks on
        // finger-down, springs back on release. onTouch returning false
        // lets the click still fire normally.
        captureBtn.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }
            }
            false
        }
    }

    private fun setupSearch() {
        findViewById<EditText>(R.id.searchInput).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                query = s.toString().trim().lowercase()
                refreshAll()
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
    }

    private fun setupSettings() {
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (_: Exception) {
            "unknown"
        }
        findViewById<TextView>(R.id.settingsInfo).text =
            "Version $versionName\n\n" +
                "Everything here lives on this device only, nothing gets uploaded " +
                "anywhere. Share a link or image from another app and Wanderer's " +
                "Grimoire shows up in the share sheet as a place to save it."
        findViewById<android.widget.Button>(R.id.manageCollectionsBtn).setOnClickListener {
            showScreen(Screen.COLLECTIONS)
        }
    }

    // Catches anything shared in from another app: Instagram/TikTok/FB
    // links share as plain text, a screenshot or saved image shares as an
    // image type. Either way we pre-fill the add dialog rather than saving
    // silently, so there's a chance to pick a folder before it's filed.
    private fun handleShareIntent(intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_SEND) return

        val mimeType = intent.type ?: ""

        if (mimeType.startsWith("image/")) {
            val uri: Uri? = readSharedUri(intent)
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
                openDialog("link", null, Prefill(title = leftover, url = foundUrl))
            } else {
                openDialog("note", null, Prefill(body = sharedText))
            }
        }

        // Consumed, don't re-fire this same shared content if the
        // activity gets recreated (e.g. on rotation).
        intent.action = null
    }

    @Suppress("DEPRECATION")
    private fun readSharedUri(intent: Intent): Uri? {
        return if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        }
    }

    private fun buildFeedRows(): List<FeedRow> {
        var list = storage.items.sortedByDescending { it.createdAt }
        if (sortAscending) list = list.reversed()

        if (activeFolder != "all") list = list.filter { it.folderId == activeFolder }

        list = when (activeType) {
            "note" -> list.filter { it.type == "note" }
            "link" -> list.filter { it.type == "link" }
            "task" -> list.filter { it.type == "task" }
            "image" -> list.filter { it.imagePath != null }
            "pinned" -> list.filter { it.pinned }
            else -> list
        }

        if (query.isNotBlank()) {
            list = list.filter { (it.title + " " + it.body).lowercase().contains(query) }
        }

        val rows = mutableListOf<FeedRow>()
        var lastLabel: String? = null
        for (item in list) {
            val label = FeedUtils.dayLabel(item.createdAt)
            if (label != lastLabel) {
                rows.add(FeedRow.Header(label))
                lastLabel = label
            }
            rows.add(FeedRow.Row(item))
        }
        return rows
    }

    private fun buildCollectionRows(): List<CollectionRow> {
        val rows = mutableListOf<CollectionRow>()
        rows.add(CollectionRow("all", "All saved", "📚", storage.items.size, deletable = false))
        storage.folders.forEach { f ->
            val count = storage.items.count { it.folderId == f.id }
            rows.add(CollectionRow(f.id, f.name, "📁", count, deletable = true))
        }
        return rows
    }

    private fun showScreen(screen: Screen) {
        activeScreen = screen
        TransitionManager.beginDelayedTransition(screenContainer, Fade().setDuration(150))
        homeScreen.visibility = if (screen == Screen.HOME) View.VISIBLE else View.GONE
        collectionsScreen.visibility = if (screen == Screen.COLLECTIONS) View.VISIBLE else View.GONE
        pinsScreen.visibility = if (screen == Screen.PINS) View.VISIBLE else View.GONE
        settingsScreen.visibility = if (screen == Screen.SETTINGS) View.VISIBLE else View.GONE

        val navMap = mapOf(
            Screen.HOME to navHome,
            Screen.COLLECTIONS to navCollections,
            Screen.PINS to navPins,
            Screen.SETTINGS to navSettings
        )
        navMap.forEach { (s, view) ->
            view.setTextColor(ContextCompat.getColor(this, if (s == screen) R.color.gold else R.color.ink_dim))
        }

        refreshAll()
    }

    private fun updateChipStyles() {
        val chips = mapOf(
            "all" to chipAll,
            "note" to chipNotes,
            "link" to chipLinks,
            "task" to chipTasks,
            "image" to chipImages,
            "pinned" to chipPins
        )
        chips.forEach { (key, chip) ->
            val active = key == activeType
            chip.setBackgroundResource(if (active) R.drawable.chip_background_active else R.drawable.chip_background)
            chip.setTextColor(ContextCompat.getColor(this, if (active) R.color.gold else R.color.ink_dim))
        }
    }

    private fun updateSectionTitle() {
        val filtered = activeType != "all" || activeFolder != "all" || query.isNotBlank()
        seeAllBtn.visibility = if (filtered) View.VISIBLE else View.GONE

        val label = when {
            query.isNotBlank() -> "SEARCH RESULTS"
            activeType == "note" -> "NOTES"
            activeType == "link" -> "LINKS"
            activeType == "task" -> "TASKS"
            activeType == "image" -> "IMAGES"
            activeType == "pinned" -> "PINNED"
            activeFolder != "all" -> storage.folders.find { it.id == activeFolder }?.name?.uppercase() ?: "RECENT"
            else -> "RECENT"
        }

        // Only crossfade when the label actually changes, this runs on
        // every keystroke in search otherwise and a transition firing that
        // often reads as jittery rather than smooth.
        if (sectionTitle.text != label) {
            TransitionManager.beginDelayedTransition(sectionTitleRow, Fade().setDuration(120))
            sectionTitle.text = label
        }
    }

    private fun refreshAll() {
        if (activeFolder == "all") {
            activeFolderBanner.visibility = View.GONE
        } else {
            val name = storage.folders.find { it.id == activeFolder }?.name ?: "Unknown"
            activeFolderLabel.text = "📁 Viewing: $name"
            activeFolderBanner.visibility = View.VISIBLE
        }

        updateSectionTitle()

        val entries = storage.items.size
        val collections = storage.folders.size
        val pinned = storage.items.count { it.pinned }
        headerStats.text = "$entries entries · $collections collections · $pinned pinned"

        feedAdapter.notifyDataSetChanged()
        pinsAdapter.notifyDataSetChanged()
        collectionAdapter.notifyDataSetChanged()

        // Replays the cascading fall-in on every refresh, not just first
        // load, so switching a filter or chip feels like a fresh reveal
        // instead of an instant content swap.
        feedList.scheduleLayoutAnimation()
        pinsList.scheduleLayoutAnimation()
        collectionsList.scheduleLayoutAnimation()

        showEmptyState(emptyView, buildFeedRows().isEmpty(), isHomeEmpty = true)
        showEmptyState(pinsEmptyView, storage.items.none { it.pinned }, isHomeEmpty = false)
    }

    // Fades the empty-state block in only on the gone-to-visible
    // transition (not replayed on every refresh while already empty), and
    // hides it instantly when content shows up, no need to animate that.
    private fun showEmptyState(view: View, shouldShow: Boolean, isHomeEmpty: Boolean) {
        val wasShowing = if (isHomeEmpty) emptyViewShowing else pinsEmptyViewShowing

        if (shouldShow && !wasShowing) {
            view.alpha = 0f
            view.visibility = View.VISIBLE
            view.animate().alpha(1f).setDuration(200).start()
        } else if (!shouldShow) {
            view.visibility = View.GONE
        } else {
            view.visibility = View.VISIBLE
        }

        if (isHomeEmpty) emptyViewShowing = shouldShow else pinsEmptyViewShowing = shouldShow
    }

    private fun deleteItem(item: Item) {
        item.imagePath?.let { ImageStore.delete(this, it) }
        storage.items.remove(item)
        storage.save()
        refreshAll()
    }

    private fun openLink(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
        }
    }

    private fun deleteFolder(id: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete collection?")
            .setMessage("Items inside stay, just unassigned.")
            .setPositiveButton("Delete") { _, _ ->
                storage.folders.removeAll { it.id == id }
                storage.items.forEach { if (it.folderId == id) it.folderId = null }
                if (activeFolder == id) activeFolder = "all"
                storage.save()
                refreshAll()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun newFolderDialog() {
        val input = EditText(this)
        input.hint = "Collection name"
        AlertDialog.Builder(this)
            .setTitle("New collection")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotBlank()) {
                    storage.folders.add(Folder(UUID.randomUUID().toString(), name))
                    storage.save()
                    refreshAll()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openQuickCapture() {
        val sheet = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.quick_capture_sheet, null)
        sheet.setContentView(view)

        view.findViewById<TextView>(R.id.captureNote).setOnClickListener {
            sheet.dismiss(); openDialog("note", null)
        }
        view.findViewById<TextView>(R.id.captureLink).setOnClickListener {
            sheet.dismiss(); openDialog("link", null)
        }
        view.findViewById<TextView>(R.id.captureTask).setOnClickListener {
            sheet.dismiss(); openDialog("task", null)
        }
        view.findViewById<TextView>(R.id.captureImage).setOnClickListener {
            sheet.dismiss(); pickImageLauncher.launch("image/*")
        }

        sheet.show()
    }

    private fun openDialog(type: String, existing: Item?, prefill: Prefill? = null) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_item, null)
        val imageField = view.findViewById<ImageView>(R.id.dialogImage)
        val titleField = view.findViewById<EditText>(R.id.dialogTitle)
        val urlField = view.findViewById<EditText>(R.id.dialogUrl)
        val bodyField = view.findViewById<EditText>(R.id.dialogBody)
        val folderSpinner = view.findViewById<Spinner>(R.id.dialogFolderSpinner)

        urlField.visibility = if (type == "link") View.VISIBLE else View.GONE

        val folderOptions = listOf("No collection") + storage.folders.map { it.name }
        folderSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, folderOptions)

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

        val dialogTitleText = if (existing != null) "Edit" else "Add ${type.replaceFirstChar { it.uppercase() }}"

        val builder = AlertDialog.Builder(this)
            .setTitle(dialogTitleText)
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
                refreshAll()
            }
            .setNegativeButton("Cancel") { _, _ ->
                if (existing == null && prefill?.imagePath != null) {
                    ImageStore.delete(this, prefill.imagePath)
                }
            }

        if (existing != null) {
            builder.setNeutralButton("Delete") { _, _ -> deleteItem(existing) }
        }

        builder.show()
    }
}

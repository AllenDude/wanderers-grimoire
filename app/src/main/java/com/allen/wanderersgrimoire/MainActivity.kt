package com.allen.wanderersgrimoire

import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.Fade
import android.transition.TransitionManager
import android.util.Patterns
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.UUID

// Fields to pre-fill the add dialog with, used when content arrives via
// the share sheet, the in-app image picker, Quick Capture's Pin shortcut,
// or a clipboard paste instead of being typed in.
data class Prefill(
    val title: String = "",
    val body: String = "",
    val url: String = "",
    val imagePath: String? = null,
    val pinned: Boolean = false
)

enum class Screen { HOME, COLLECTIONS, PINS, SETTINGS, THEME, THEME_EDITOR }

class MainActivity : AppCompatActivity() {

    private lateinit var storage: Storage
    private lateinit var theme: Theme
    private lateinit var editorTheme: Theme

    private var activeScreen: Screen = Screen.HOME
    private var activeType: String = "all" // all, note, link, task, image, pinned
    private var activeFolder: String = "all"
    private var query: String = ""
    private var sortAscending: Boolean = false

    private var emptyViewShowing = false
    private var pinsEmptyViewShowing = false

    private lateinit var feedAdapter: FeedAdapter
    private lateinit var pinsAdapter: FeedAdapter
    private lateinit var collectionAdapter: CollectionAdapter

    private val swatchPalette = listOf(
        0xFFD4AF6A.toInt(), 0xFFE0A530.toInt(), 0xFFE07A3F.toInt(), 0xFFC1503D.toInt(),
        0xFFD97C9C.toInt(), 0xFFC1507A.toInt(), 0xFF8E7CF0.toInt(), 0xFFA78BD1.toInt(),
        0xFF5FA8E8.toInt(), 0xFF4FC3D9.toInt(), 0xFF6FAE5E.toInt(), 0xFF4C7A6B.toInt(),
        0xFFC6C6D0.toInt(), 0xFF9A9AA6.toInt(), 0xFFEDE3C8.toInt(), 0xFF9C7A42.toInt(),
        0xFF14131C.toInt(), 0xFF1E1D2A.toInt(), 0xFF0A0A0C.toInt(), 0xFFEFE6D2.toInt()
    )

    // Views
    private lateinit var rootLayout: ViewGroup
    private lateinit var screenContainer: ViewGroup
    private lateinit var homeScreen: View
    private lateinit var collectionsScreen: View
    private lateinit var pinsScreen: View
    private lateinit var settingsScreen: View
    private lateinit var themeScreen: View
    private lateinit var themeEditorScreen: View
    private lateinit var headerTitle: TextView
    private lateinit var headerTagline: TextView
    private lateinit var headerBookIcon: ImageView
    private lateinit var headerStats: TextView
    private lateinit var searchIcon: ImageView
    private lateinit var searchInput: EditText
    private lateinit var sortToggle: ImageView
    private lateinit var sectionTitleRow: ViewGroup
    private lateinit var sectionTitle: TextView
    private lateinit var seeAllBtn: TextView
    private lateinit var emptyView: View
    private lateinit var pinsEmptyView: View
    private lateinit var activeFolderBanner: View
    private lateinit var activeFolderLabel: TextView
    private lateinit var clearFolderFilter: TextView
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
    private lateinit var navCapture: ImageView
    private lateinit var feedList: RecyclerView
    private lateinit var pinsList: RecyclerView
    private lateinit var collectionsList: RecyclerView
    private lateinit var collectionsHeader: TextView
    private lateinit var pinsHeader: TextView
    private lateinit var settingsHeader: TextView
    private lateinit var settingsInfo: TextView
    private lateinit var manageCollectionsBtn: Button
    private lateinit var customizeThemeBtn: Button
    private lateinit var themeBackBtn: TextView
    private lateinit var presetsContainer: ViewGroup
    private lateinit var customThemeRow: TextView
    private lateinit var editorBackBtn: TextView
    private lateinit var colorRowsContainer: ViewGroup
    private lateinit var buttonStyleRow: ViewGroup
    private lateinit var cardStyleRow: ViewGroup
    private lateinit var cornerRadiusSeek: SeekBar
    private lateinit var cornerRadiusLabel: TextView
    private lateinit var saveThemeBtn: Button

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

        theme = ThemeManager.current(this)
        storage = Storage(this)
        storage.load()

        bindViews()
        applyTheme()
        setupFeed()
        setupPins()
        setupCollections()
        setupChips()
        setupNav()
        setupSearch()
        setupSettings()
        setupThemeScreen()
        setupThemeEditor()

        showScreen(Screen.HOME)
        handleShareIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()
    }

    private fun sizedTinted(resId: Int, sizeDp: Int, color: Int): Drawable {
        val d = androidx.core.content.ContextCompat.getDrawable(this, resId)!!.mutate()
        d.setTint(color)
        val px = dp(sizeDp)
        d.setBounds(0, 0, px, px)
        return d
    }

    private fun circleDrawable(color: Int): Drawable {
        val d = GradientDrawable()
        d.shape = GradientDrawable.OVAL
        d.setColor(color)
        return d
    }

    private fun bindViews() {
        rootLayout = findViewById(R.id.rootLayout)
        screenContainer = findViewById(R.id.screenContainer)
        homeScreen = findViewById(R.id.homeScreen)
        collectionsScreen = findViewById(R.id.collectionsScreen)
        pinsScreen = findViewById(R.id.pinsScreen)
        settingsScreen = findViewById(R.id.settingsScreen)
        themeScreen = findViewById(R.id.themeScreen)
        themeEditorScreen = findViewById(R.id.themeEditorScreen)
        headerTitle = findViewById(R.id.headerTitle)
        headerTagline = findViewById(R.id.headerTagline)
        headerBookIcon = findViewById(R.id.headerBookIcon)
        headerStats = findViewById(R.id.headerStats)
        searchIcon = findViewById(R.id.searchIcon)
        searchInput = findViewById(R.id.searchInput)
        sortToggle = findViewById(R.id.sortToggle)
        sectionTitleRow = findViewById(R.id.sectionTitleRow)
        sectionTitle = findViewById(R.id.sectionTitle)
        seeAllBtn = findViewById(R.id.seeAllBtn)
        emptyView = findViewById(R.id.emptyView)
        pinsEmptyView = findViewById(R.id.pinsEmptyView)
        activeFolderBanner = findViewById(R.id.activeFolderBanner)
        activeFolderLabel = findViewById(R.id.activeFolderLabel)
        clearFolderFilter = findViewById(R.id.clearFolderFilter)
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
        navCapture = findViewById(R.id.navCapture)
        feedList = findViewById(R.id.feedList)
        pinsList = findViewById(R.id.pinsList)
        collectionsList = findViewById(R.id.collectionsList)
        collectionsHeader = findViewById(R.id.collectionsHeader)
        pinsHeader = findViewById(R.id.pinsHeader)
        settingsHeader = findViewById(R.id.settingsHeader)
        settingsInfo = findViewById(R.id.settingsInfo)
        manageCollectionsBtn = findViewById(R.id.manageCollectionsBtn)
        themeBackBtn = findViewById(R.id.themeBackBtn)
        presetsContainer = findViewById(R.id.presetsContainer)
        customThemeRow = findViewById(R.id.customThemeRow)
        editorBackBtn = findViewById(R.id.editorBackBtn)
        colorRowsContainer = findViewById(R.id.colorRowsContainer)
        buttonStyleRow = findViewById(R.id.buttonStyleRow)
        cardStyleRow = findViewById(R.id.cardStyleRow)
        cornerRadiusSeek = findViewById(R.id.cornerRadiusSeek)
        cornerRadiusLabel = findViewById(R.id.cornerRadiusLabel)
        saveThemeBtn = findViewById(R.id.saveThemeBtn)

        // Settings gets an extra "Customize theme" entry, added here in
        // code since it's not part of the original static settings layout.
        customizeThemeBtn = Button(this)
        customizeThemeBtn.text = "Customize theme"
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.topMargin = dp(12)
        customizeThemeBtn.layoutParams = params
        (settingsScreen as LinearLayout).addView(customizeThemeBtn, (settingsScreen as LinearLayout).indexOfChild(manageCollectionsBtn) + 1)
    }

    // Re-colors the app's chrome (not the AlertDialogs, those still use the
    // system default style) from the active Theme. Called once at startup;
    // the activity recreates itself whenever the theme actually changes,
    // so this always reflects the current theme on every screen.
    private fun applyTheme() {
        rootLayout.setBackgroundColor(theme.background)
        headerTitle.setTextColor(theme.text)
        headerTagline.setTextColor(theme.textDim)
        headerBookIcon.setColorFilter(theme.accent)
        headerStats.setTextColor(theme.textDim)

        searchInput.background = ThemeManager.cardDrawable(this, theme, theme.surface)
        searchInput.setTextColor(theme.text)
        searchInput.setHintTextColor(theme.textDim)
        searchIcon.setColorFilter(theme.textDim)
        sortToggle.setColorFilter(theme.textDim)

        activeFolderLabel.setTextColor(theme.accent)
        clearFolderFilter.setTextColor(theme.textDim)

        sectionTitle.setTextColor(theme.accent)
        seeAllBtn.setTextColor(theme.textDim)

        findViewById<TextView>(R.id.emptyGlyph).setTextColor(theme.accent)
        findViewById<TextView>(R.id.emptyTitle).setTextColor(theme.text)
        findViewById<TextView>(R.id.emptySubtitle).setTextColor(theme.textDim)
        findViewById<TextView>(R.id.emptyAddBtn).apply {
            setTextColor(theme.accent)
            background = ThemeManager.buttonDrawable(this@MainActivity, theme, theme.surface, theme.accent)
        }
        findViewById<TextView>(R.id.pinsEmptyGlyph).setTextColor(theme.accent)
        findViewById<TextView>(R.id.pinsEmptyTitle).setTextColor(theme.text)
        findViewById<TextView>(R.id.pinsEmptySubtitle).setTextColor(theme.textDim)

        collectionsHeader.setTextColor(theme.text)
        pinsHeader.setTextColor(theme.text)
        settingsHeader.setTextColor(theme.text)
        settingsInfo.setTextColor(theme.textDim)

        val navBar = navHome.parent as View
        navBar.setBackgroundColor(theme.surface)
        navCapture.background = themedCapsule(theme.accent)
        navCapture.setColorFilter(theme.background)

        val buttonTint = ColorStateList.valueOf(theme.accent)
        manageCollectionsBtn.backgroundTintList = buttonTint
        customizeThemeBtn.backgroundTintList = buttonTint
        saveThemeBtn.backgroundTintList = buttonTint
        findViewById<Button>(R.id.newCollectionBtn).backgroundTintList = buttonTint
    }

    private fun themedCapsule(color: Int): Drawable {
        val oval = GradientDrawable()
        oval.shape = GradientDrawable.OVAL
        oval.setColor(color)
        return oval
    }

    private fun setupFeed() {
        feedAdapter = FeedAdapter(
            theme = theme,
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

        clearFolderFilter.setOnClickListener {
            activeFolder = "all"
            refreshAll()
        }

        sortToggle.setOnClickListener {
            sortAscending = !sortAscending
            it.alpha = if (sortAscending) 1f else 0.6f
            refreshAll()
        }

        seeAllBtn.setOnClickListener {
            activeType = "all"
            activeFolder = "all"
            query = ""
            searchInput.setText("")
            updateChipStyles()
            refreshAll()
        }

        findViewById<TextView>(R.id.emptyAddBtn).setOnClickListener { openQuickCapture() }
    }

    private fun setupPins() {
        pinsAdapter = FeedAdapter(
            theme = theme,
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
            theme = theme,
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

        findViewById<Button>(R.id.newCollectionBtn).setOnClickListener { newFolderDialog() }
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

        navCapture.setOnClickListener { openQuickCapture() }
        // A small press-in squish, like a real Material FAB: shrinks on
        // finger-down, springs back on release. onTouch returning false
        // lets the click still fire normally.
        navCapture.setOnTouchListener { view, event ->
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
        searchInput.addTextChangedListener(object : TextWatcher {
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
        settingsInfo.text =
            "Version $versionName\n\n" +
                "Everything here lives on this device only, nothing gets uploaded " +
                "anywhere. Share a link or image from another app and Wanderer's " +
                "Grimoire shows up in the share sheet as a place to save it."
        manageCollectionsBtn.setOnClickListener { showScreen(Screen.COLLECTIONS) }
        customizeThemeBtn.setOnClickListener { showScreen(Screen.THEME) }
    }

    private fun setupThemeScreen() {
        themeBackBtn.setOnClickListener { showScreen(Screen.SETTINGS) }
        themeBackBtn.setTextColor(theme.textDim)
        customThemeRow.setOnClickListener {
            populateThemeEditor()
            showScreen(Screen.THEME_EDITOR)
        }
        customThemeRow.setTextColor(theme.accent)
        customThemeRow.background = ThemeManager.cardDrawable(this, theme, theme.surface)

        presetsContainer.removeAllViews()
        Theme.PRESETS.forEach { preset ->
            val row = layoutInflater.inflate(R.layout.theme_preset_row, presetsContainer, false)
            row.background = ThemeManager.cardDrawable(this, theme, theme.surface)
            row.findViewById<TextView>(R.id.presetName).text = preset.name
            row.findViewById<TextView>(R.id.presetName).setTextColor(theme.text)
            row.findViewById<View>(R.id.dot1).background = circleDrawable(preset.primary)
            row.findViewById<View>(R.id.dot2).background = circleDrawable(preset.secondary)
            row.findViewById<View>(R.id.dot3).background = circleDrawable(preset.background)
            row.findViewById<View>(R.id.dot4).background = circleDrawable(preset.surface)
            val check = row.findViewById<TextView>(R.id.presetCheck)
            check.setTextColor(theme.accent)
            check.visibility = if (theme.name == preset.name) View.VISIBLE else View.INVISIBLE
            row.setOnClickListener {
                ThemeManager.applyPreset(this, preset.name)
                recreate()
            }
            presetsContainer.addView(row)
        }
    }

    private fun setupThemeEditor() {
        editorBackBtn.setOnClickListener { showScreen(Screen.THEME) }
        editorBackBtn.setTextColor(theme.textDim)
        saveThemeBtn.setOnClickListener {
            ThemeManager.saveCustom(this, editorTheme)
            recreate()
        }
    }

    private fun populateThemeEditor() {
        editorTheme = theme

        colorRowsContainer.removeAllViews()
        addColorRow("Primary", editorTheme.primary) { c -> editorTheme = editorTheme.copy(primary = c) }
        addColorRow("Secondary", editorTheme.secondary) { c -> editorTheme = editorTheme.copy(secondary = c) }
        addColorRow("Background", editorTheme.background) { c -> editorTheme = editorTheme.copy(background = c) }
        addColorRow("Surface", editorTheme.surface) { c -> editorTheme = editorTheme.copy(surface = c) }
        addColorRow("Text", editorTheme.text) { c -> editorTheme = editorTheme.copy(text = c) }
        addColorRow("Accent", editorTheme.accent) { c -> editorTheme = editorTheme.copy(accent = c) }

        buttonStyleRow.removeAllViews()
        listOf("rounded" to "Rounded", "sharp" to "Sharp", "pill" to "Pill", "tab" to "Tab").forEach { (key, label) ->
            addStyleOption(buttonStyleRow, label, editorTheme.buttonStyle == key) {
                editorTheme = editorTheme.copy(buttonStyle = key)
                populateThemeEditor()
            }
        }

        cardStyleRow.removeAllViews()
        listOf("flat" to "Flat", "outlined" to "Outlined", "elevated" to "Elevated").forEach { (key, label) ->
            addStyleOption(cardStyleRow, label, editorTheme.cardStyle == key) {
                editorTheme = editorTheme.copy(cardStyle = key)
                populateThemeEditor()
            }
        }

        cornerRadiusSeek.max = 24
        cornerRadiusSeek.progress = editorTheme.cornerRadiusDp
        cornerRadiusLabel.text = "${editorTheme.cornerRadiusDp}dp"
        cornerRadiusLabel.setTextColor(theme.textDim)
        cornerRadiusSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                cornerRadiusLabel.text = "${value}dp"
                editorTheme = editorTheme.copy(cornerRadiusDp = value)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun addColorRow(label: String, color: Int, onChange: (Int) -> Unit): View {
        val row = layoutInflater.inflate(R.layout.theme_color_row, colorRowsContainer, false)
        val labelView = row.findViewById<TextView>(R.id.rowLabel)
        val hexView = row.findViewById<TextView>(R.id.rowHex)
        val swatch = row.findViewById<View>(R.id.rowSwatch)
        labelView.text = label
        labelView.setTextColor(theme.text)
        hexView.text = String.format("#%06X", 0xFFFFFF and color)
        hexView.setTextColor(theme.textDim)
        val swatchBg = GradientDrawable()
        swatchBg.cornerRadius = dp(6).toFloat()
        swatchBg.setColor(color)
        swatch.background = swatchBg
        row.setOnClickListener {
            openSwatchPicker(color) { picked ->
                onChange(picked)
                populateThemeEditor()
            }
        }
        colorRowsContainer.addView(row)
        return row
    }

    private fun addStyleOption(container: ViewGroup, label: String, selected: Boolean, onClick: () -> Unit) {
        val tv = TextView(this)
        tv.text = label
        tv.textSize = 13f
        tv.setPadding(dp(14), dp(10), dp(14), dp(10))
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.marginEnd = dp(8)
        tv.layoutParams = params
        tv.setTextColor(if (selected) theme.accent else theme.textDim)
        tv.background = ThemeManager.buttonDrawable(this, editorTheme, theme.surface, if (selected) theme.accent else null)
        tv.setOnClickListener { onClick() }
        container.addView(tv)
    }

    private fun openSwatchPicker(current: Int, onPick: (Int) -> Unit) {
        val grid = GridLayout(this)
        grid.columnCount = 5
        grid.setPadding(dp(12), dp(12), dp(12), dp(12))

        val dialog = AlertDialog.Builder(this)
            .setTitle("Choose a color")
            .setView(grid)
            .setNegativeButton("Cancel", null)
            .create()

        for (color in swatchPalette) {
            val cell = View(this)
            val params = GridLayout.LayoutParams()
            params.width = dp(44)
            params.height = dp(44)
            params.setMargins(dp(6), dp(6), dp(6), dp(6))
            cell.layoutParams = params
            val bg = GradientDrawable()
            bg.shape = GradientDrawable.OVAL
            bg.setColor(color)
            if (color == current) bg.setStroke(dp(3), theme.text)
            cell.background = bg
            cell.setOnClickListener {
                onPick(color)
                dialog.dismiss()
            }
            grid.addView(cell)
        }
        dialog.show()
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
        rows.add(CollectionRow("all", "All saved", storage.items.size, deletable = false))
        storage.folders.forEach { f ->
            val count = storage.items.count { it.folderId == f.id }
            rows.add(CollectionRow(f.id, f.name, count, deletable = true))
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
        themeScreen.visibility = if (screen == Screen.THEME) View.VISIBLE else View.GONE
        themeEditorScreen.visibility = if (screen == Screen.THEME_EDITOR) View.VISIBLE else View.GONE

        val navMap = mapOf(
            Screen.HOME to navHome,
            Screen.COLLECTIONS to navCollections,
            Screen.PINS to navPins,
            Screen.SETTINGS to navSettings
        )
        navMap.forEach { (s, view) ->
            val active = s == screen || (s == Screen.SETTINGS && (screen == Screen.THEME || screen == Screen.THEME_EDITOR))
            val color = if (active) theme.accent else theme.textDim
            view.setTextColor(color)
            val iconRes = when (s) {
                Screen.HOME -> R.drawable.ic_home
                Screen.COLLECTIONS -> R.drawable.ic_collection
                Screen.PINS -> R.drawable.ic_pin_outline
                else -> R.drawable.ic_settings
            }
            view.setCompoundDrawables(null, sizedTinted(iconRes, 20, color), null, null)
            view.compoundDrawablePadding = dp(4)
        }

        refreshAll()
    }

    private fun updateChipStyles() {
        val chips = listOf(
            Triple("all", chipAll, null),
            Triple("note", chipNotes, R.drawable.ic_note),
            Triple("link", chipLinks, R.drawable.ic_link),
            Triple("task", chipTasks, R.drawable.ic_task),
            Triple("image", chipImages, R.drawable.ic_image),
            Triple("pinned", chipPins, R.drawable.ic_pin)
        )
        chips.forEach { (key, chip, iconRes) ->
            val active = key == activeType
            val color = if (active) theme.accent else theme.textDim
            chip.background = ThemeManager.buttonDrawable(
                this, theme,
                if (active) theme.surface else theme.surface,
                if (active) theme.accent else null
            )
            chip.setTextColor(color)
            if (iconRes != null) {
                chip.setCompoundDrawables(sizedTinted(iconRes, 15, color), null, null, null)
                chip.compoundDrawablePadding = dp(5)
            }
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
            activeFolderLabel.text = "Viewing: $name"
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

    private fun newFolderDialog(onCreated: ((String) -> Unit)? = null) {
        val input = EditText(this)
        input.hint = "Collection name"
        AlertDialog.Builder(this)
            .setTitle("New collection")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotBlank()) {
                    val folder = Folder(UUID.randomUUID().toString(), name)
                    storage.folders.add(folder)
                    storage.save()
                    refreshAll()
                    onCreated?.invoke(folder.id)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openQuickCapture() {
        val sheet = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.quick_capture_sheet, null)
        sheet.setContentView(view)

        val folderSpinner = view.findViewById<Spinner>(R.id.quickCaptureFolderSpinner)
        val folderOptions = listOf("No collection") + storage.folders.map { it.name }
        folderSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, folderOptions)
        val preselectIdx = storage.folders.indexOfFirst { it.id == activeFolder }
        folderSpinner.setSelection(if (preselectIdx >= 0) preselectIdx + 1 else 0)

        fun selectedFolderId(): String? {
            val pos = folderSpinner.selectedItemPosition
            return if (pos <= 0) null else storage.folders[pos - 1].id
        }

        fun iconFor(id: Int, resId: Int) {
            view.findViewById<TextView>(id).apply {
                setCompoundDrawables(null, sizedTinted(resId, 26, theme.accent), null, null)
                compoundDrawablePadding = dp(6)
            }
        }
        iconFor(R.id.captureNote, R.drawable.ic_note)
        iconFor(R.id.captureLink, R.drawable.ic_link)
        iconFor(R.id.captureImage, R.drawable.ic_image)
        iconFor(R.id.capturePin, R.drawable.ic_pin)
        iconFor(R.id.captureTask, R.drawable.ic_task)
        iconFor(R.id.captureClipboard, R.drawable.ic_clipboard)

        view.findViewById<TextView>(R.id.captureNote).setOnClickListener {
            sheet.dismiss(); openDialog("note", null, Prefill(), presetFolder = selectedFolderId())
        }
        view.findViewById<TextView>(R.id.captureLink).setOnClickListener {
            sheet.dismiss(); openDialog("link", null, Prefill(), presetFolder = selectedFolderId())
        }
        view.findViewById<TextView>(R.id.captureTask).setOnClickListener {
            sheet.dismiss(); openDialog("task", null, Prefill(), presetFolder = selectedFolderId())
        }
        view.findViewById<TextView>(R.id.captureImage).setOnClickListener {
            sheet.dismiss(); pickImageLauncher.launch("image/*")
        }
        view.findViewById<TextView>(R.id.capturePin).setOnClickListener {
            // "Pin" is a shortcut, not a separate type: it opens the Link
            // dialog with pinned already turned on, since in this app
            // pinning is a flag any item can have, not its own content type.
            sheet.dismiss(); openDialog("link", null, Prefill(pinned = true), presetFolder = selectedFolderId())
        }
        view.findViewById<TextView>(R.id.captureClipboard).setOnClickListener {
            sheet.dismiss(); captureFromClipboard(selectedFolderId())
        }
        view.findViewById<TextView>(R.id.quickCaptureNewFolder).setOnClickListener {
            sheet.dismiss()
            newFolderDialog()
        }

        sheet.show()
    }

    private fun captureFromClipboard(presetFolder: String?) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip == null || clip.itemCount == 0) return
        val text = clip.getItemAt(0).coerceToText(this)?.toString()?.trim()
        if (text.isNullOrBlank()) return

        val matcher = Patterns.WEB_URL.matcher(text)
        if (matcher.find()) {
            val foundUrl = matcher.group()
            val leftover = text.replace(foundUrl, "").trim()
            openDialog("link", null, Prefill(title = leftover, url = foundUrl), presetFolder = presetFolder)
        } else {
            openDialog("note", null, Prefill(body = text), presetFolder = presetFolder)
        }
    }

    private fun openDialog(type: String, existing: Item?, prefill: Prefill? = null, presetFolder: String? = null) {
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
        val startPinned = existing?.pinned ?: prefill?.pinned ?: false

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
            val targetFolder = presetFolder ?: activeFolder
            val idx = storage.folders.indexOfFirst { it.id == targetFolder }
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
                            pinned = startPinned,
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

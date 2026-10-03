package com.allen.wanderersgrimoire

// A theme is a complete set of design tokens: colors, per-content-type card
// surface tints, and the shape language (button/card style, corner radius).
// Every screen and adapter reads from the currently active Theme instead of
// hardcoded resources, so switching themes actually propagates everywhere.
data class Theme(
    val name: String,
    val background: Int,
    val surface: Int,
    val text: Int,
    val textDim: Int,
    val primary: Int,
    val secondary: Int,
    val accent: Int,
    val danger: Int,
    val noteSurface: Int,
    val linkSurface: Int,
    val taskSurface: Int,
    val buttonStyle: String = "rounded", // rounded, sharp, pill, tab
    val cardStyle: String = "flat",      // flat, outlined, elevated
    val cornerRadiusDp: Int = 12
) {
    companion object {
        val MIDNIGHT = Theme(
            name = "Midnight",
            background = 0xFF14131C.toInt(),
            surface = 0xFF1E1D2A.toInt(),
            text = 0xFFEDE3C8.toInt(),
            textDim = 0xFF9B92A8.toInt(),
            primary = 0xFFD4AF6A.toInt(),
            secondary = 0xFFA78BD1.toInt(),
            accent = 0xFFD4AF6A.toInt(),
            danger = 0xFFC1503D.toInt(),
            noteSurface = 0xFF2A2820.toInt(),
            linkSurface = 0xFF1E2235.toInt(),
            taskSurface = 0xFF1A2B22.toInt()
        )

        val ARCANE = Theme(
            name = "Arcane",
            background = 0xFF161229.toInt(),
            surface = 0xFF231C3D.toInt(),
            text = 0xFFE8E2F5.toInt(),
            textDim = 0xFF9C8FC2.toInt(),
            primary = 0xFF8E7CF0.toInt(),
            secondary = 0xFF5FA8E8.toInt(),
            accent = 0xFF8E7CF0.toInt(),
            danger = 0xFFD1507A.toInt(),
            noteSurface = 0xFF2E2645.toInt(),
            linkSurface = 0xFF1F2A4D.toInt(),
            taskSurface = 0xFF24263F.toInt()
        )

        val EMBER = Theme(
            name = "Ember",
            background = 0xFF1C1210.toInt(),
            surface = 0xFF2A1C17.toInt(),
            text = 0xFFF2E2D6.toInt(),
            textDim = 0xFFB08E7D.toInt(),
            primary = 0xFFE07A3F.toInt(),
            secondary = 0xFFC1503D.toInt(),
            accent = 0xFFE07A3F.toInt(),
            danger = 0xFFC1503D.toInt(),
            noteSurface = 0xFF332419.toInt(),
            linkSurface = 0xFF2A1F22.toInt(),
            taskSurface = 0xFF2E2117.toInt()
        )

        val FOREST = Theme(
            name = "Forest",
            background = 0xFF101810.toInt(),
            surface = 0xFF1A2A1C.toInt(),
            text = 0xFFE4EDE0.toInt(),
            textDim = 0xFF8FA88E.toInt(),
            primary = 0xFF6FAE5E.toInt(),
            secondary = 0xFF4C7A6B.toInt(),
            accent = 0xFF6FAE5E.toInt(),
            danger = 0xFFC1503D.toInt(),
            noteSurface = 0xFF28301E.toInt(),
            linkSurface = 0xFF1C2A26.toInt(),
            taskSurface = 0xFF203322.toInt()
        )

        val OCEAN = Theme(
            name = "Ocean",
            background = 0xFF0C161E.toInt(),
            surface = 0xFF15242F.toInt(),
            text = 0xFFE2EEF2.toInt(),
            textDim = 0xFF89A7B3.toInt(),
            primary = 0xFF4FC3D9.toInt(),
            secondary = 0xFF5B8FBF.toInt(),
            accent = 0xFF4FC3D9.toInt(),
            danger = 0xFFD9705A.toInt(),
            noteSurface = 0xFF1E2A30.toInt(),
            linkSurface = 0xFF172A3A.toInt(),
            taskSurface = 0xFF1A2E2C.toInt()
        )

        val PARCHMENT = Theme(
            name = "Parchment",
            background = 0xFFEFE6D2.toInt(),
            surface = 0xFFF7F1E3.toInt(),
            text = 0xFF2B241A.toInt(),
            textDim = 0xFF7A6D55.toInt(),
            primary = 0xFF9C7A42.toInt(),
            secondary = 0xFF8C6FA8.toInt(),
            accent = 0xFF9C7A42.toInt(),
            danger = 0xFFA1432F.toInt(),
            noteSurface = 0xFFE8DCC0.toInt(),
            linkSurface = 0xFFE3DEEA.toInt(),
            taskSurface = 0xFFDCE3D0.toInt()
        )

        val OBSIDIAN = Theme(
            name = "Obsidian",
            background = 0xFF0A0A0C.toInt(),
            surface = 0xFF17171B.toInt(),
            text = 0xFFEDEDF0.toInt(),
            textDim = 0xFF8C8C93.toInt(),
            primary = 0xFFC6C6D0.toInt(),
            secondary = 0xFF9A9AA6.toInt(),
            accent = 0xFFC6C6D0.toInt(),
            danger = 0xFFD9664F.toInt(),
            noteSurface = 0xFF201F24.toInt(),
            linkSurface = 0xFF1C1C22.toInt(),
            taskSurface = 0xFF1E1E22.toInt()
        )

        val ROSEWOOD = Theme(
            name = "Rosewood",
            background = 0xFF1C0F15.toInt(),
            surface = 0xFF2B1821.toInt(),
            text = 0xFFF2E2E6.toInt(),
            textDim = 0xFFB5899A.toInt(),
            primary = 0xFFD97C9C.toInt(),
            secondary = 0xFFC1507A.toInt(),
            accent = 0xFFD97C9C.toInt(),
            danger = 0xFFC1503D.toInt(),
            noteSurface = 0xFF331D26.toInt(),
            linkSurface = 0xFF2A1A28.toInt(),
            taskSurface = 0xFF2E1C20.toInt()
        )

        val PRESETS = listOf(MIDNIGHT, ARCANE, EMBER, FOREST, OCEAN, PARCHMENT, OBSIDIAN, ROSEWOOD)
    }
}

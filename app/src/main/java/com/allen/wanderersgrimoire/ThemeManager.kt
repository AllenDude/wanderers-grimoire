package com.allen.wanderersgrimoire

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue

object ThemeManager {
    private const val PREFS = "grimoire_theme"

    fun current(context: Context): Theme {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val presetName = prefs.getString("preset", "Midnight")

        if (presetName == "Custom") {
            return Theme(
                name = "Custom",
                background = prefs.getInt("c_background", Theme.MIDNIGHT.background),
                surface = prefs.getInt("c_surface", Theme.MIDNIGHT.surface),
                text = prefs.getInt("c_text", Theme.MIDNIGHT.text),
                textDim = Theme.MIDNIGHT.textDim,
                primary = prefs.getInt("c_primary", Theme.MIDNIGHT.primary),
                secondary = prefs.getInt("c_secondary", Theme.MIDNIGHT.secondary),
                accent = prefs.getInt("c_accent", Theme.MIDNIGHT.accent),
                danger = Theme.MIDNIGHT.danger,
                noteSurface = Theme.MIDNIGHT.noteSurface,
                linkSurface = Theme.MIDNIGHT.linkSurface,
                taskSurface = Theme.MIDNIGHT.taskSurface,
                buttonStyle = prefs.getString("c_buttonStyle", "rounded") ?: "rounded",
                cardStyle = prefs.getString("c_cardStyle", "flat") ?: "flat",
                cornerRadiusDp = prefs.getInt("c_cornerRadius", 12)
            )
        }

        return Theme.PRESETS.find { it.name == presetName } ?: Theme.MIDNIGHT
    }

    fun applyPreset(context: Context, name: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("preset", name)
            .apply()
    }

    fun saveCustom(context: Context, theme: Theme) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("preset", "Custom")
            .putInt("c_background", theme.background)
            .putInt("c_surface", theme.surface)
            .putInt("c_text", theme.text)
            .putInt("c_primary", theme.primary)
            .putInt("c_secondary", theme.secondary)
            .putInt("c_accent", theme.accent)
            .putString("c_buttonStyle", theme.buttonStyle)
            .putString("c_cardStyle", theme.cardStyle)
            .putInt("c_cornerRadius", theme.cornerRadiusDp)
            .apply()
    }

    private fun dp(context: Context, value: Int): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics)
    }

    // Builds a button/chip background from the theme's button style token.
    // "pill" ignores the radius setting (always fully rounded, capsule
    // shape); the others use the theme's corner radius. "tab" rounds only
    // the top corners, giving a filed-tab silhouette instead of a plain
    // rounded rect.
    fun buttonDrawable(context: Context, theme: Theme, fillColor: Int, strokeColor: Int? = null, strokeWidthDp: Int = 1): GradientDrawable {
        val d = GradientDrawable()
        d.setColor(fillColor)
        if (strokeColor != null) d.setStroke(dp(context, strokeWidthDp).toInt(), strokeColor)

        when (theme.buttonStyle) {
            "sharp" -> d.cornerRadius = 0f
            "pill" -> d.cornerRadius = dp(context, 100)
            "tab" -> d.cornerRadii = floatArrayOf(
                dp(context, theme.cornerRadiusDp), dp(context, theme.cornerRadiusDp),
                dp(context, theme.cornerRadiusDp), dp(context, theme.cornerRadiusDp),
                0f, 0f,
                0f, 0f
            )
            else -> d.cornerRadius = dp(context, theme.cornerRadiusDp) // rounded
        }
        return d
    }

    // Builds a card background from the theme's card style token.
    // "outlined" adds a hairline border in the theme's text-dim tone;
    // "elevated" lightens the fill slightly to read as raised, since real
    // drop shadows are kept to a minimum throughout the app.
    fun cardDrawable(context: Context, theme: Theme, fillColor: Int): GradientDrawable {
        val d = GradientDrawable()
        d.cornerRadius = dp(context, theme.cornerRadiusDp)
        when (theme.cardStyle) {
            "outlined" -> {
                d.setColor(fillColor)
                d.setStroke(dp(context, 1).toInt(), theme.textDim and 0x80FFFFFF.toInt())
            }
            "elevated" -> {
                d.setColor(lighten(fillColor, 0.06f))
            }
            else -> d.setColor(fillColor) // flat
        }
        return d
    }

    private fun lighten(color: Int, amount: Float): Int {
        val r = ((color shr 16) and 0xFF)
        val g = ((color shr 8) and 0xFF)
        val b = (color and 0xFF)
        val a = ((color shr 24) and 0xFF)
        val nr = (r + (255 - r) * amount).toInt().coerceIn(0, 255)
        val ng = (g + (255 - g) * amount).toInt().coerceIn(0, 255)
        val nb = (b + (255 - b) * amount).toInt().coerceIn(0, 255)
        return (a shl 24) or (nr shl 16) or (ng shl 8) or nb
    }
}

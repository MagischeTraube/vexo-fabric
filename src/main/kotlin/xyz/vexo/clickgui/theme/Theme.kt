package xyz.vexo.clickgui.theme

import xyz.vexo.clickgui.GuiPrefs
import xyz.vexo.clickgui.components.ClickGuiColor
import java.awt.Color

object Theme {

    fun Color.withAlpha(a: Int): Color = Color(red, green, blue, a.coerceIn(0, 255))

    private fun mix(a: Color, b: Color, t: Float): Color {
        val inv = 1f - t
        return Color(
            (a.red * inv + b.red * t).toInt().coerceIn(0, 255),
            (a.green * inv + b.green * t).toInt().coerceIn(0, 255),
            (a.blue * inv + b.blue * t).toInt().coerceIn(0, 255)
        )
    }

    private fun glass(base: Color, glassAlpha: Int): Color =
        if (GuiPrefs.blurGlass) base.withAlpha(glassAlpha) else base

    fun accent(): Color = GuiPrefs.accentColor
    fun accentHover(): Color = mix(accent(), Color.BLACK, 0.18f)

    fun header(): Color = glass(ClickGuiColor.HEADER_COLOR, 205)
    fun sidebar(): Color = glass(ClickGuiColor.BACKGROUND_COLOR, 175)
    fun panel(): Color = glass(ClickGuiColor.BACKGROUND_COLOR, 160)
    fun panelDark(): Color = glass(ClickGuiColor.DARK_BACKGROUND_COLOR, 185)

    fun card(): Color = glass(ClickGuiColor.CARD_BG, 190)
    fun cardHover(): Color = glass(ClickGuiColor.CARD_HOVER, 215)
    /** Active card: accent-tinted so it follows the chosen accent color. */
    fun cardActive(): Color = glass(mix(ClickGuiColor.CARD_BG, accent(), 0.28f), 215)

    fun inputBackground(): Color = glass(ClickGuiColor.GRAY_TEXT_INPUT_BACKGROUND_COLOR, 200)
    fun overlayScrim(): Color = Color(0, 0, 0, 150)

    fun divider(): Color = ClickGuiColor.SEPARATOR_COLOR.withAlpha(140)
    /** Faint top-edge highlight that sells the glass look. */
    fun glassEdge(): Color = Color(255, 255, 255, if (GuiPrefs.blurGlass) 24 else 12)

    /** Soft accent glow color; transparent when glow is disabled. */
    fun glow(alpha: Int = 70): Color = if (GuiPrefs.glow) accent().withAlpha(alpha) else accent().withAlpha(0)
    /** Faux drop-shadow color; transparent when shadows are disabled. */
    fun shadow(alpha: Int = 90): Color = Color(0, 0, 0, if (GuiPrefs.shadows) alpha else 0)

    fun textPrimary(): Color = ClickGuiColor.WHITE_TEXT_COLOR
    fun textMuted(): Color = ClickGuiColor.GRAY_TEXT_COLOR
    fun textOnAccent(): Color = ClickGuiColor.WHITE_TEXT_COLOR

    fun toggleOff(): Color = ClickGuiColor.TOGGLE_OFF_COLOR
    fun sliderTrack(): Color = ClickGuiColor.SLIDER_TRACK_COLOR
    fun handle(): Color = ClickGuiColor.WHITE_SLIDER_HANDLE_COLOR
    fun success(): Color = ClickGuiColor.SUCCESS_COLOR
    fun star(): Color = Color(250, 204, 21) // gold for favorites
}

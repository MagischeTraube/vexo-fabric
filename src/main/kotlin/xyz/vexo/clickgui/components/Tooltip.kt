package xyz.vexo.clickgui.components

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import xyz.vexo.clickgui.GuiPrefs
import xyz.vexo.clickgui.gpx
import xyz.vexo.clickgui.guiScale
import xyz.vexo.clickgui.theme.Theme

object Tooltip {
    private var holder: UIContainer? = null
    private var background: UIComponent? = null
    private var label: UIText? = null

    fun attach(window: Window) {
        detach()
        val h = UIContainer().constrain {
            width = 20.gpx()
            height = 20.gpx()
        } childOf window

        val bg = UIRoundedRectangle(5f).constrain {
            width = 100.percent()
            height = 100.percent()
        }.setColor(Theme.panelDark()) childOf h
        bg.enableEffect(OutlineEffect(Theme.glow(120), 1f))

        val text = UIText("").constrain {
            x = 7.gpx()
            y = CenterConstraint()
            textScale = 1.gpx()
        }.apply { setColor(Theme.textPrimary()) } childOf h

        h.hide(instantly = true)
        holder = h
        background = bg
        label = text
    }

    fun show(text: String, anchor: UIComponent) {
        if (!GuiPrefs.tooltips || text.isBlank()) return
        val h = holder ?: return
        label?.setText(text)

        val win = Window.of(h)
        val width = (text.width() + 14f) * guiScale
        val height = 20f * guiScale
        h.setWidth(width.pixels())
        var x = anchor.getLeft()
        if (x + width > win.getWidth()) x = (win.getWidth() - width - 4f).coerceAtLeast(4f)
        var y = anchor.getBottom() + 4f * guiScale
        if (y + height > win.getHeight()) y = (anchor.getTop() - height - 4f * guiScale).coerceAtLeast(4f)

        h.setX(x.pixels())
        h.setY(y.pixels())
        h.unhide()
    }

    fun hide() {
        holder?.hide(instantly = true)
    }

    fun detach() {
        holder?.let { it.parent.removeChild(it) }
        holder = null
        background = null
        label = null
    }
}

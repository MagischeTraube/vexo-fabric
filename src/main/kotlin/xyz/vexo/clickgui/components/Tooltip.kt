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
import xyz.vexo.clickgui.theme.Theme.withAlpha

object Tooltip {
    private var holder: UIContainer? = null
    private var background: UIComponent? = null
    private var label: UIText? = null

    fun attach(window: Window) {
        detach()
        val h = UIContainer().constrain {
            width = 20.gpx()
            height = 24.gpx()
        } childOf window

        val bg = UIRoundedRectangle(6f).constrain {
            width = 100.percent()
            height = 100.percent()
        }.setColor(Theme.panelDark().withAlpha(255)) childOf h

        bg.enableEffect(OutlineEffect(Theme.glow(120), 1f))

        val text = UIText("").constrain {
            x = 9.gpx()
            y = CenterConstraint()
            textScale = 1.15.gpx()
        }.apply {
            setColor(Theme.textPrimary())
        } childOf h

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

        val width = (text.width() + 20f) * guiScale
        val height = 26f * guiScale

        h.setWidth(width.pixels())
        h.setHeight(height.pixels())

        val gapX = 8f * guiScale
        val gapY = 5f * guiScale

        var x = anchor.getLeft() + gapX
        var y = anchor.getBottom() + gapY

        if (x + width > win.getWidth()) {
            x = (win.getWidth() - width - 4f).coerceAtLeast(4f)
        }

        if (y + height > win.getHeight()) {
            y = (anchor.getTop() - height - gapY).coerceAtLeast(4f)
        }

        x = x.coerceIn(4f, (win.getWidth() - width - 4f).coerceAtLeast(4f))
        y = y.coerceIn(4f, (win.getHeight() - height - 4f).coerceAtLeast(4f))

        h.setX(x.pixels())
        h.setY(y.pixels())

        Window.enqueueRenderOperation {
            h.unhide()
        }
    }

    fun hide() {
        val h = holder ?: return

        Window.enqueueRenderOperation {
            h.hide(instantly = true)
        }
    }

    fun detach() {
        val h = holder ?: return

        Window.enqueueRenderOperation {
            h.parent.removeChild(h)
        }

        holder = null
        background = null
        label = null
    }
}

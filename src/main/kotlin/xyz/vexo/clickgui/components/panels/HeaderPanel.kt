package xyz.vexo.clickgui.components.panels

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.input.UITextInput
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import xyz.vexo.Vexo
import xyz.vexo.clickgui.gpx
import xyz.vexo.clickgui.gsib
import xyz.vexo.clickgui.theme.Theme
import xyz.vexo.clickgui.theme.Theme.withAlpha
import xyz.vexo.clickgui.theme.colorTo

/**
 * Glass header: brand + version, a focus-glowing search field, and a button that opens the GUI
 * effects overlay.
 */
class HeaderPanel(
    private val onSearchUpdate: (String) -> Unit,
    private val onOpenSettings: () -> Unit = {}
) : UIContainer() {

    init {
        constrain {
            width = 100.percent()
            height = 44.gpx()
        }

        setupHeader()
    }

    private fun setupHeader() {
        UIRoundedRectangle(5f).constrain {
            width = 100.percent()
            height = 100.percent()
        }.setColor(Theme.header()) childOf this

        // Faint top highlight + bottom divider sell the frosted-glass edge.
        UIBlock(Theme.glassEdge()).constrain {
            width = 100.percent()
            height = 1.gpx()
        } childOf this

        UIBlock(Theme.divider()).constrain {
            y = 0.pixels(true)
            width = 100.percent()
            height = 1.gpx()
        } childOf this

        UIText("VEXO").constrain {
            x = 14.gpx()
            y = CenterConstraint()
            textScale = 2.gpx()
        }.setColor(Theme.accent()) childOf this

        UIText("v${Vexo.version}").constrain {
            x = gsib(6f)
            y = CenterConstraint() + 3.gpx()
            textScale = 1.gpx()
        }.setColor(Theme.textMuted()) childOf this

        createSettingsButton() childOf this
        createSearchBox() childOf this
    }

    private fun createSettingsButton(): UIComponent {
        val button = UIContainer().constrain {
            x = 12.gpx(true)
            y = CenterConstraint()
            width = 26.gpx()
            height = 26.gpx()
        }

        val bg = UIRoundedRectangle(7f).constrain {
            width = 100.percent()
            height = 100.percent()
        }.setColor(Theme.card()) childOf button

        // A simple, always-renderable "sliders/menu" glyph: three stacked rounded lines.
        listOf(8f, 13f, 18f).forEach { yy ->
            UIRoundedRectangle(1f).constrain {
                x = CenterConstraint()
                y = yy.gpx()
                width = 12.gpx()
                height = 2.gpx()
            }.setColor(Theme.textPrimary()) childOf button
        }

        button.onMouseEnter { bg.colorTo(Theme.accent()) }
        button.onMouseLeave { bg.colorTo(Theme.card()) }
        button.onMouseClick {
            it.stopPropagation()
            onOpenSettings()
        }

        return button
    }

    private fun createSearchBox(): UIComponent {
        val container = UIContainer().constrain {
            x = 46.gpx(true)
            y = CenterConstraint()
            width = 200.gpx()
            height = 26.gpx()
        }

        // Glow layer behind the field, lit on focus.
        val glow = UIRoundedRectangle(8f).constrain {
            x = (-2).gpx()
            y = (-2).gpx()
            width = 100.percent() + 4.gpx()
            height = 100.percent() + 4.gpx()
        }.setColor(Theme.accent().withAlpha(0)) childOf container

        UIRoundedRectangle(6f).constrain {
            width = 100.percent()
            height = 100.percent()
        }.setColor(Theme.inputBackground()) childOf container

        val searchBox = UITextInput("Search...").constrain {
            x = 8.gpx()
            y = CenterConstraint() + 6.gpx()
            width = 100.percent() - 16.gpx()
            height = 100.percent()
        }.apply {
            setTextScale(1.3f.gpx())
        } childOf container

        searchBox.onMouseClick {
            searchBox.grabWindowFocus()
            it.stopPropagation()
        }

        searchBox.onFocus { glow.colorTo(Theme.glow(70)) }
        searchBox.onFocusLost { glow.colorTo(Theme.accent().withAlpha(0)) }

        searchBox.onUpdate { text ->
            onSearchUpdate(text)
        }

        return container
    }
}

package xyz.vexo.clickgui.components.panels

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.components.input.UITextInput
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import xyz.vexo.Vexo
import xyz.vexo.clickgui.components.ClickGuiColor

/**
 * Header panel component
 */
class HeaderPanel(
    private val onSearchUpdate: (String) -> Unit
) : UIContainer() {

    init {
        constrain {
            width = 100.percent()
            height = 40.pixels()
        }

        setupHeader()
    }

    private fun setupHeader() {
        UIBlock(ClickGuiColor.HEADER_COLOR).constrain {
            width = 100.percent()
            height = 100.percent()
        } childOf this

        UIText("VEXO").constrain {
            x = 10.pixels()
            y = CenterConstraint()
            textScale = 2.pixels()
        }.setColor(ClickGuiColor.ACCENT_COLOR) childOf this

        UIText("v${Vexo.version}").constrain {
            x = SiblingConstraint(5f)
            y = CenterConstraint()
            textScale = 1.pixels()
        }.setColor(ClickGuiColor.GRAY_TEXT_COLOR) childOf this

        createSearchBox() childOf this
    }

    private fun createSearchBox(): UIComponent {
        val container = UIContainer().constrain {
            x = 10.pixels(true)
            y = CenterConstraint()
            width = 200.pixels()
            height = 25.pixels()
        }

        UIBlock(ClickGuiColor.GRAY_TEXT_INPUT_BACKGROUND_COLOR).constrain {
            width = 100.percent()
            height = 100.percent()
        } childOf container

        container.enableEffect(OutlineEffect(ClickGuiColor.ACCENT_COLOR, 1f))

        val searchBox = UITextInput("Search...").constrain {
            x = 5.pixels()
            y = CenterConstraint() + 6.pixels()
            width = 100.percent() - 10.pixels()
            height = 100.percent()
        }.apply {
            setTextScale(1.3f.pixels())
        } childOf container

        searchBox.onMouseClick {
            searchBox.grabWindowFocus()
            it.stopPropagation()
        }

        searchBox.onUpdate { text ->
            onSearchUpdate(text)
        }

        return container
    }
}

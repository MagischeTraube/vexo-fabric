package xyz.vexo.clickgui.components.settings

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.*
import xyz.vexo.config.impl.SelectorSetting
import xyz.vexo.clickgui.components.ClickGuiColor

/**
 * Selector setting component
 *
 * @param setting The selector setting to be displayed
 */
class SelectorSettingComponent(
    private val setting: SelectorSetting
) : UIContainer() {

    private var dropdownOpen = false
    private var dropdownContainer: UIContainer? = null

    init {
        constrain {
            width = 100.percent()
            height = 40.pixels()
        }

        UIText(setting.name).constrain {
            x = 10.pixels()
            y = CenterConstraint()
        } childOf this

        val selectorButton = UIContainer().constrain {
            x = 10.pixels(true)
            y = CenterConstraint()
            width = 100.pixels()
            height = 25.pixels()
        } childOf this

        val selectorBackground = UIBlock(ClickGuiColor.LIGHT_GRAY_BACKGROUND).constrain {
            width = 100.percent()
            height = 100.percent()
        } childOf selectorButton

        val selectorText = UIText(setting.getCurrentValue()).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
        } childOf selectorButton

        selectorButton.onMouseEnter {
            selectorBackground.setColor(ClickGuiColor.HOVER_COLOR)
        }

        selectorButton.onMouseLeave {
            selectorBackground.setColor(ClickGuiColor.LIGHT_GRAY_BACKGROUND)
        }

        selectorButton.onMouseClick { event ->
            event.stopPropagation()

            if (dropdownOpen) {
                closeDropdown()
            } else {
                openDropdown(selectorButton, selectorText)
            }
        }
    }

    private fun openDropdown(selectorButton: UIContainer, selectorText: UIText) {
        val window = Window.of(this)

        val dropdown = UIContainer().constrain {
            x = selectorButton.getLeft().pixels()
            y = (selectorButton.getTop() + 27).pixels()
            width = 100.pixels()
        } childOf window

        setting.options.forEachIndexed { index, option ->
            val optionContainer = UIContainer().constrain {
                y = (index * 25).pixels()
                width = 100.percent()
                height = 25.pixels()
            } childOf dropdown

            val optionBackground = UIBlock(ClickGuiColor.LIGHT_GRAY_BACKGROUND).constrain {
                width = 100.percent()
                height = 100.percent()
            } childOf optionContainer

            val optionText = UIText(option).constrain {
                x = CenterConstraint()
                y = CenterConstraint()
            } childOf optionContainer

            optionContainer.onMouseEnter {
                optionBackground.setColor(ClickGuiColor.HOVER_COLOR)
            }

            optionContainer.onMouseLeave {
                optionBackground.setColor(ClickGuiColor.LIGHT_GRAY_BACKGROUND)
            }

            optionContainer.onMouseClick {
                setting.updateValue(option)
                selectorText.setText(option)
                closeDropdown()
            }
        }

        dropdown.constrain {
            height = (setting.options.size * 25).pixels()
        }

        Window.of(this).onMouseClick {
            closeDropdown()
        }

        dropdownContainer = dropdown
        dropdownOpen = true
    }

    private fun closeDropdown() {
        dropdownContainer?.let {
            it.parent.removeChild(it)
        }
        dropdownContainer = null
        dropdownOpen = false
    }
}

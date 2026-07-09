package xyz.vexo.clickgui.components.settings

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import xyz.vexo.clickgui.gpx
import xyz.vexo.clickgui.guiScale
import xyz.vexo.clickgui.theme.Theme
import xyz.vexo.clickgui.theme.colorTo
import xyz.vexo.config.impl.SelectorSetting

/**
 * Selector setting component — glass button opening a glass dropdown; the active option is accented.
 */
class SelectorSettingComponent(
    private val setting: SelectorSetting,
    private val onUpdate: (() -> Unit)? = null
) : UIContainer() {

    private var dropdownOpen = false
    private var dropdownContainer: UIContainer? = null

    init {
        constrain {
            width = 100.percent()
            height = 40.gpx()
        }

        UIText(setting.name).constrain {
            x = 14.gpx()
            y = CenterConstraint()
            textScale = 1.gpx()
        }.setColor(Theme.textPrimary()) childOf this

        val selectorButton = UIContainer().constrain {
            x = 14.gpx(true)
            y = CenterConstraint()
            width = 110.gpx()
            height = 25.gpx()
        } childOf this

        val selectorBackground = UIRoundedRectangle(6f).constrain {
            width = 100.percent()
            height = 100.percent()
        }.setColor(Theme.card()) childOf selectorButton

        val selectorText = UIText(setting.getCurrentValue()).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            textScale = 1.gpx()
        }.apply { setColor(Theme.textPrimary()) } childOf selectorButton

        selectorButton.onMouseEnter { selectorBackground.colorTo(Theme.cardHover()) }
        selectorButton.onMouseLeave { selectorBackground.colorTo(Theme.card()) }

        selectorButton.onMouseClick { event ->
            event.stopPropagation()
            if (dropdownOpen) closeDropdown() else openDropdown(selectorButton, selectorText)
        }
    }

    private fun openDropdown(selectorButton: UIContainer, selectorText: UIText) {
        val window = Window.of(this)

        // Row height scales with the GUI; getLeft()/getTop() are already in real (scaled) pixels.
        val rowHeight = 25f * guiScale
        val dropdown = UIContainer().constrain {
            x = selectorButton.getLeft().pixels()
            y = (selectorButton.getTop() + rowHeight + 2f * guiScale).pixels()
            width = 110.gpx()
            height = (setting.options.size * rowHeight).pixels()
        } childOf window
        dropdown.enableEffect(OutlineEffect(Theme.glow(120), 1f))

        setting.options.forEachIndexed { index, option ->
            val optionContainer = UIContainer().constrain {
                y = (index * rowHeight).pixels()
                width = 100.percent()
                height = rowHeight.pixels()
            } childOf dropdown

            val isActive = option == setting.getCurrentValue()
            val optionBackground = UIRoundedRectangle(if (index == 0 || index == setting.options.lastIndex) 5f else 0f).constrain {
                width = 100.percent()
                height = 100.percent()
            }.setColor(if (isActive) Theme.cardActive() else Theme.panelDark()) childOf optionContainer

            UIText(option).constrain {
                x = CenterConstraint()
                y = CenterConstraint()
                textScale = 1.gpx()
            }.setColor(if (isActive) Theme.accent() else Theme.textPrimary()) childOf optionContainer

            optionContainer.onMouseEnter {
                if (option != setting.getCurrentValue()) optionBackground.colorTo(Theme.cardHover())
            }
            optionContainer.onMouseLeave {
                if (option != setting.getCurrentValue()) optionBackground.colorTo(Theme.panelDark())
            }
            optionContainer.onMouseClick {
                setting.updateValue(option)
                selectorText.setText(option)
                onUpdate?.invoke()
                closeDropdown()
            }
        }

        Window.of(this).onMouseClick { closeDropdown() }

        dropdownContainer = dropdown
        dropdownOpen = true
    }

    private fun closeDropdown() {
        dropdownContainer?.let { it.parent.removeChild(it) }
        dropdownContainer = null
        dropdownOpen = false
    }
}

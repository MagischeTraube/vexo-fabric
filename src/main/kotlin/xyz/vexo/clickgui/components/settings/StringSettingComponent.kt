package xyz.vexo.clickgui.components.settings

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.input.UITextInput
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.elementa.UIComponent
import xyz.vexo.clickgui.components.ClickGuiColor
import xyz.vexo.config.impl.StringSetting

/**
 * String setting component
 *
 * @param setting The string setting to be displayed
 */
class StringSettingComponent(
    private val setting: StringSetting
) : UIContainer() {

    init {
        constrain {
            width = 100.percent()
            height = 60.pixels()
        }

        UIText(setting.name).constrain {
            x = 10.pixels()
            y = 5.pixels()
        } childOf this

        createTextInput() childOf this
    }

    private fun createTextInput(): UIComponent {
        val container = UIContainer().constrain {
            x = 10.pixels()
            y = 25.pixels()
            width = 100.percent() - 20.pixels()
            height = 25.pixels()
        }

        UIBlock(ClickGuiColor.GRAY_TEXT_INPUT_BACKGROUND_COLOR).constrain {
            width = 100.percent()
            height = 100.percent()
        } childOf container

        container.enableEffect(OutlineEffect(ClickGuiColor.ACCENT_COLOR, 1f))

        val textInput = UITextInput(setting.getCurrentValue()).constrain {
            x = 5.pixels()
            y = CenterConstraint() + 8.pixels()
            width = 100.percent() - 10.pixels()
            height = 100.percent()
        }.apply {
            setTextScale(1.2f.pixels())
        } childOf container

        textInput.onMouseClick {
            textInput.grabWindowFocus()
            it.stopPropagation()
        }

        textInput.onUpdate { text ->
            setting.updateValue(text)
        }

        return container
    }
}

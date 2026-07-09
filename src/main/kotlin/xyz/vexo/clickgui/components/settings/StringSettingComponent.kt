package xyz.vexo.clickgui.components.settings

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.input.UITextInput
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import xyz.vexo.clickgui.gpx
import xyz.vexo.clickgui.theme.Theme
import xyz.vexo.clickgui.theme.Theme.withAlpha
import xyz.vexo.clickgui.theme.colorTo
import xyz.vexo.config.impl.StringSetting

/**
 * String setting component — glass input field with a focus glow.
 */
class StringSettingComponent(
    private val setting: StringSetting
) : UIContainer() {

    init {
        constrain {
            width = 100.percent()
            height = 60.gpx()
        }

        UIText(setting.name).constrain {
            x = 14.gpx()
            y = 8.gpx()
            textScale = 1.gpx()
        }.setColor(Theme.textPrimary()) childOf this

        createTextInput() childOf this
    }

    private fun createTextInput(): UIComponent {
        val container = UIContainer().constrain {
            x = 14.gpx()
            y = 28.gpx()
            width = 100.percent() - 28.gpx()
            height = 26.gpx()
        }

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

        val textInput = UITextInput(setting.getCurrentValue()).constrain {
            x = 8.gpx()
            y = CenterConstraint() + 8.gpx()
            width = 100.percent() - 16.gpx()
            height = 100.percent()
        }.apply {
            setTextScale(1.2f.gpx())
        } childOf container

        textInput.onMouseClick {
            textInput.grabWindowFocus()
            it.stopPropagation()
        }

        textInput.onFocus { glow.colorTo(Theme.glow(70)) }

        textInput.onUpdate { text ->
            setting.updateValue(text.replace("&&", "§"))
        }

        textInput.onFocusLost {
            glow.colorTo(Theme.accent().withAlpha(0))
            textInput.setText(setting.getCurrentValue())
        }

        return container
    }
}

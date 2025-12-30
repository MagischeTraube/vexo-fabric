package xyz.vexo.clickgui.components.settings

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.*
import xyz.vexo.clickgui.components.ClickGuiColor
import xyz.vexo.config.impl.BooleanSetting

/**
 * Boolean setting component
 *
 * @param setting The boolean setting to be displayed
 * @param onUpdate The callback to be called when the setting is updated
 */
class BooleanSettingComponent(
    private val setting: BooleanSetting,
    private val onUpdate: (() -> Unit)? = null
) : UIContainer() {

    init {
        constrain {
            width = 100.percent()
            height = 40.pixels()
        }

        UIText(setting.name).constrain {
            x = 10.pixels()
            y = CenterConstraint()
        } childOf this

        val toggle = UIContainer().constrain {
            x = 10.pixels(true)
            y = CenterConstraint()
            width = 40.pixels()
            height = 20.pixels()
        } childOf this

        val toggleBackground = UIBlock(if (setting.getCurrentValue()) ClickGuiColor.ACCENT_COLOR else ClickGuiColor.LIGHT_GRAY_BACKGROUND).constrain {
            width = 100.percent()
            height = 100.percent()
        } childOf toggle

        val toggleSlider = UIBlock(ClickGuiColor.WHITE_SLIDER_HANDLE_COLOR).constrain {
            x = if (setting.getCurrentValue()) 22.pixels() else 2.pixels()
            y = 2.pixels()
            width = 16.pixels()
            height = 16.pixels()
        } childOf toggle

        toggle.onMouseClick {
            setting.updateValue(!setting.getCurrentValue())
            toggleBackground.setColor(if (setting.getCurrentValue()) ClickGuiColor.ACCENT_COLOR else ClickGuiColor.LIGHT_GRAY_BACKGROUND)
            toggleSlider.setX(if (setting.getCurrentValue()) 22.pixels() else 2.pixels())
            onUpdate?.invoke()
        }
    }
}

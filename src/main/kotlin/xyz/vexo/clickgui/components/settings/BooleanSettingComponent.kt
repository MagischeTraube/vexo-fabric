package xyz.vexo.clickgui.components.settings

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.*
import xyz.vexo.clickgui.gpx
import xyz.vexo.clickgui.theme.Theme
import xyz.vexo.clickgui.theme.colorTo
import xyz.vexo.clickgui.theme.xTo
import xyz.vexo.config.impl.BooleanSetting

/**
 * Boolean setting component — animated pill toggle (sliding knob + color fade).
 */
class BooleanSettingComponent(
    private val setting: BooleanSetting,
    private val onUpdate: (() -> Unit)? = null
) : UIContainer() {

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

        val toggle = UIContainer().constrain {
            x = 14.gpx(true)
            y = CenterConstraint()
            width = 40.gpx()
            height = 20.gpx()
        } childOf this

        val toggleBackground = UIRoundedRectangle(10f).constrain {
            width = 100.percent()
            height = 100.percent()
        }.setColor(if (setting.getCurrentValue()) Theme.accent() else Theme.toggleOff()) childOf toggle

        val toggleSlider = UIRoundedRectangle(8f).constrain {
            x = if (setting.getCurrentValue()) 22.gpx() else 2.gpx()
            y = 2.gpx()
            width = 16.gpx()
            height = 16.gpx()
        }.setColor(Theme.handle()) childOf toggle

        toggle.onMouseClick {
            setting.updateValue(!setting.getCurrentValue())
            val on = setting.getCurrentValue()
            toggleBackground.colorTo(if (on) Theme.accent() else Theme.toggleOff())
            toggleSlider.xTo(if (on) 22.gpx() else 2.gpx())
            onUpdate?.invoke()
        }
    }
}

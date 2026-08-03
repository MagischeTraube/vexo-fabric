package xyz.vexo.clickgui.components.settings

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.*
import xyz.vexo.clickgui.gpx
import xyz.vexo.clickgui.theme.Theme
import xyz.vexo.clickgui.theme.colorTo
import xyz.vexo.config.impl.ButtonSetting

class ButtonSettingComponent(
    private val setting: ButtonSetting
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

        val button = UIContainer().constrain {
            x = 14.gpx(true)
            y = CenterConstraint()
            width = 90.gpx()
            height = 25.gpx()
        } childOf this

        val background = UIRoundedRectangle(6f).constrain {
            width = 100.percent()
            height = 100.percent()
        }.setColor(Theme.accent()) childOf button

        UIText(setting.label).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            textScale = 1.gpx()
        }.setColor(Theme.textOnAccent()) childOf button

        button.onMouseEnter { background.colorTo(Theme.accentHover()) }
        button.onMouseLeave { background.colorTo(Theme.accent()) }
        button.onMouseClick { setting.onClick() }
    }
}

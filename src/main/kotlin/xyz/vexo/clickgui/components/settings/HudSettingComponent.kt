package xyz.vexo.clickgui.components.settings

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.*
import xyz.vexo.clickgui.gpx
import xyz.vexo.clickgui.theme.Theme
import xyz.vexo.clickgui.theme.colorTo
import xyz.vexo.config.impl.HudSetting

/**
 * Hud setting component — glass "Move" button opening the HUD editor.
 */
class HudSettingComponent(
    private val setting: HudSetting,
    private val onMoveClick: () -> Unit
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

        val moveButton = UIContainer().constrain {
            x = 14.gpx(true)
            y = CenterConstraint()
            width = 64.gpx()
            height = 25.gpx()
        } childOf this

        val moveBackground = UIRoundedRectangle(6f).constrain {
            width = 100.percent()
            height = 100.percent()
        }.setColor(Theme.accent()) childOf moveButton

        UIText("Move").constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            textScale = 1.gpx()
        }.setColor(Theme.textOnAccent()) childOf moveButton

        moveButton.onMouseEnter { moveBackground.colorTo(Theme.accentHover()) }
        moveButton.onMouseLeave { moveBackground.colorTo(Theme.accent()) }
        moveButton.onMouseClick { onMoveClick() }
    }
}

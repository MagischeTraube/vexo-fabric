package xyz.vexo.clickgui.components.settings

import java.awt.Color
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.*
import xyz.vexo.config.impl.HudSetting

/**
 * Hud setting component
 *
 * @param setting The hud setting to be displayed
 * @param onMoveClick The callback to be called when the move button is clicked
 */
class HudSettingComponent(
    private val setting: HudSetting,
    private val onMoveClick: () -> Unit
) : UIContainer() {

    private val accentColor = Color(40, 120, 255)
    private val hoverColor = Color(30, 90, 200)

    init {
        constrain {
            width = 100.percent()
            height = 40.pixels()
        }

        UIText(setting.name).constrain {
            x = 10.pixels()
            y = CenterConstraint()
        } childOf this

        val moveButton = UIContainer().constrain {
            x = 10.pixels(true)
            y = CenterConstraint()
            width = 60.pixels()
            height = 25.pixels()
        } childOf this

        val moveBackground = UIBlock(accentColor).constrain {
            width = 100.percent()
            height = 100.percent()
        } childOf moveButton

        UIText("Move").constrain {
            x = CenterConstraint()
            y = CenterConstraint()
        } childOf moveButton

        moveButton.onMouseEnter {
            moveBackground.setColor(hoverColor)
        }

        moveButton.onMouseLeave {
            moveBackground.setColor(accentColor)
        }

        moveButton.onMouseClick {
            onMoveClick()
        }
    }
}

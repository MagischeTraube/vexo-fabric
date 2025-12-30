package xyz.vexo.clickgui.components.settings

import org.lwjgl.glfw.GLFW
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.*
import xyz.vexo.config.impl.KeybindSetting
import xyz.vexo.clickgui.components.ClickGuiColor

/**
 * Keybind setting component
 *
 * @param setting The keybind setting to be displayed
 */
class KeybindSettingComponent(
    private val setting: KeybindSetting
) : UIContainer() {

    private var keyListenerRegistered = false

    init {
        constrain {
            width = 100.percent()
            height = 40.pixels()
        }

        UIText(setting.name).constrain {
            x = 10.pixels()
            y = CenterConstraint()
        } childOf this

        val keybindButton = UIContainer().constrain {
            x = 10.pixels(true)
            y = CenterConstraint()
            width = 80.pixels()
            height = 25.pixels()
        } childOf this

        val keybindBackground = UIBlock(ClickGuiColor.LIGHT_GRAY_BACKGROUND).constrain {
            width = 100.percent()
            height = 100.percent()
        } childOf keybindButton

        val keybindText = UIText(getKeyName(setting.getCurrentValue())).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
        } childOf keybindButton

        keybindButton.onMouseClick {
            if (!keyListenerRegistered) {
                registerKeyListener(keybindText, keybindBackground)
                keyListenerRegistered = true
            }

            setting.listening = !setting.listening
            keybindText.setText(if (setting.listening) "..." else getKeyName(setting.getCurrentValue()))
            keybindBackground.setColor(if (setting.listening) ClickGuiColor.ACCENT_COLOR else ClickGuiColor.LIGHT_GRAY_BACKGROUND)
        }

        keybindButton.onMouseEnter {
            if (!setting.listening) {
                keybindBackground.setColor(ClickGuiColor.HOVER_COLOR)
            }
        }

        keybindButton.onMouseLeave {
            if (!setting.listening) {
                keybindBackground.setColor(ClickGuiColor.LIGHT_GRAY_BACKGROUND)
            }
        }
    }

    private fun registerKeyListener(keybindText: UIText, keybindBackground: UIBlock) {
        val window = Window.of(this)
        window.onKeyType { typedChar, keyCode ->
            if (setting.listening) {
                when (keyCode) {
                    GLFW.GLFW_KEY_ESCAPE,
                    GLFW.GLFW_KEY_DELETE,
                    GLFW.GLFW_KEY_BACKSPACE -> {
                        setting.updateValue(-1)
                        keybindText.setText(getKeyName(-1))
                        keybindBackground.setColor(ClickGuiColor.LIGHT_GRAY_BACKGROUND)
                    }
                    else -> {
                        setting.updateValue(keyCode)
                        keybindText.setText(getKeyName(keyCode))
                        keybindBackground.setColor(ClickGuiColor.LIGHT_GRAY_BACKGROUND)
                    }
                }
                setting.listening = false
            }
        }
    }


    private fun getKeyName(keyCode: Int): String {
        return when {
            keyCode == -1 -> "None"
            else -> {
                val keyName = GLFW.glfwGetKeyName(keyCode, 0)
                keyName?.uppercase() ?: when (keyCode) {
                    GLFW.GLFW_KEY_SPACE -> "SPACE"
                    GLFW.GLFW_KEY_ESCAPE -> "ESC"
                    GLFW.GLFW_KEY_ENTER -> "ENTER"
                    GLFW.GLFW_KEY_TAB -> "TAB"
                    GLFW.GLFW_KEY_BACKSPACE -> "BACK"
                    GLFW.GLFW_KEY_DELETE -> "DEL"
                    GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT"
                    GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT"
                    GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL"
                    GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL"
                    GLFW.GLFW_KEY_LEFT_ALT -> "LALT"
                    GLFW.GLFW_KEY_RIGHT_ALT -> "RALT"
                    else -> "Key $keyCode"
                }
            }
        }
    }
}

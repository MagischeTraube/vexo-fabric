package xyz.vexo.clickgui.components.settings

import com.mojang.blaze3d.platform.InputConstants
import org.lwjgl.sdl.SDLKeyboard
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.*
import xyz.vexo.clickgui.gpx
import xyz.vexo.clickgui.theme.Theme
import xyz.vexo.clickgui.theme.colorTo
import xyz.vexo.config.impl.KeybindSetting

/**
 * Keybind setting component — glass button that turns accent while listening for a key.
 */
class KeybindSettingComponent(
    private val setting: KeybindSetting
) : UIContainer() {

    init {
        setting.listening = false

        constrain {
            width = 100.percent()
            height = 40.gpx()
        }

        UIText(setting.name).constrain {
            x = 14.gpx()
            y = CenterConstraint()
            textScale = 1.gpx()
        }.setColor(Theme.textPrimary()) childOf this

        val keybindButton = UIContainer().constrain {
            x = 14.gpx(true)
            y = CenterConstraint()
            width = 80.gpx()
            height = 25.gpx()
        } childOf this

        val keybindBackground = UIRoundedRectangle(6f).constrain {
            width = 100.percent()
            height = 100.percent()
        }.setColor(Theme.card()) childOf keybindButton

        val keybindText = UIText(getKeyName(setting.getCurrentValue())).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            textScale = 1.gpx()
        }.apply { setColor(Theme.textPrimary()) } childOf keybindButton

        fun stopListening() {
            setting.listening = false
            keybindText.setText(getKeyName(setting.getCurrentValue()))
            keybindBackground.colorTo(Theme.card())
        }

        keybindButton.onMouseClick { event ->
            event.stopPropagation()
            if (setting.listening) {
                keybindButton.releaseWindowFocus()
                return@onMouseClick
            }
            setting.listening = true
            keybindButton.grabWindowFocus()
            keybindText.setText("...")
            keybindBackground.colorTo(Theme.accent())
        }

        keybindButton.onKeyType { _, keyCode ->
            if (!setting.listening) return@onKeyType
            // UniversalCraft hands us an SDL keycode, but Minecraft (InputConstants, KeyEvent.key(),
            // isKeyDown) works with SDL scancodes — store the scancode so it matches KeybindEvent.
            val scancode = SDLKeyboard.SDL_GetScancodeFromKey(keyCode, null)
            val unbind = scancode == InputConstants.KEY_ESCAPE || scancode == InputConstants.KEY_DELETE || scancode == InputConstants.KEY_BACKSPACE
            setting.updateValue(if (unbind) -1 else scancode)
            keybindButton.releaseWindowFocus()
        }

        keybindButton.onFocusLost { stopListening() }

        keybindButton.onMouseEnter {
            if (!setting.listening) keybindBackground.colorTo(Theme.cardHover())
        }

        keybindButton.onMouseLeave {
            if (!setting.listening) keybindBackground.colorTo(Theme.card())
        }
    }

    private companion object {
        val specialKeys = mapOf(
            InputConstants.KEY_SPACE to "SPACE",
            InputConstants.KEY_ESCAPE to "ESC",
            InputConstants.KEY_RETURN to "ENTER",
            InputConstants.KEY_TAB to "TAB",
            InputConstants.KEY_BACKSPACE to "BACK",
            InputConstants.KEY_DELETE to "DEL",
            InputConstants.KEY_LSHIFT to "LSHIFT",
            InputConstants.KEY_RSHIFT to "RSHIFT",
            InputConstants.KEY_LCONTROL to "LCTRL",
            InputConstants.KEY_RCONTROL to "RCTRL",
            InputConstants.KEY_LALT to "LALT",
            InputConstants.KEY_RALT to "RALT",
            InputConstants.KEY_UP to "UP",
            InputConstants.KEY_DOWN to "DOWN",
            InputConstants.KEY_LEFT to "LEFT",
            InputConstants.KEY_RIGHT to "RIGHT"
        )
    }

    private fun getKeyName(keyCode: Int): String {
        if (keyCode == -1) return "None"
        specialKeys[keyCode]?.let { return it }
        val name = SDLKeyboard.SDL_GetScancodeName(keyCode)
        return if (name.isNullOrEmpty()) "Key $keyCode" else name.uppercase()
    }
}

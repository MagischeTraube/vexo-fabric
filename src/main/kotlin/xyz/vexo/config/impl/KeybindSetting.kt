package xyz.vexo.config.impl

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import xyz.vexo.config.Setting
import xyz.vexo.clickgui.ClickGui
import xyz.vexo.Vexo.mc

/**
 * A keybind setting that can be configured in a module.
 *
 * @param name The name of the setting.
 * @param default The default value of the setting. Defaults to -1 (no keybind).
 */
class KeybindSetting(
    name: String,
    description: String = "",
    default: Int = -1
) : Setting<Int>(name, description, value = default) {
    companion object {
        @JvmStatic
        fun isListeningForKeybinds(): Boolean {
            val screen = mc.screen
            return screen is ClickGui &&
                    screen.activeKeybindSettings.any { it.listening }
        }
    }

    var listening: Boolean = false

    override fun toJson() = JsonPrimitive(getCurrentValue())

    override fun fromJson(json: JsonElement) {
        if (json.isJsonPrimitive && json.asJsonPrimitive.isNumber) {
            updateValue(json.asInt)
        }
    }
}

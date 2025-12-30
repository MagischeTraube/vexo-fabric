package xyz.vexo.config.impl

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import xyz.vexo.config.Setting
import xyz.vexo.hud.HudElement
import xyz.vexo.hud.HudManager

/**
 * A hud setting that can be configured in a module.
 *
 * @param name The name of the setting.
 * @param description A description of the setting. Defaults to an empty string.
 * @param defaultText The default text of the setting. Defaults to the name of the setting.
 */
class HudSetting(
    name: String,
    description: String = "",
    defaultText: String = name,
    defaultVisibility: Boolean = false,
) : Setting<HudElement>(name, description, HudElement(name, defaultText, visible = defaultVisibility)) {

    init {
        HudManager.registerHud(this)
    }

    fun setPos(x: Int, y: Int) {
        getCurrentValue().x = x
        getCurrentValue().y = y
    }

    fun setScale(scale: Float) {
        getCurrentValue().scale = scale
    }

    fun setText(text: String) {
        getCurrentValue().text = text
    }

    fun setVisible(visible: Boolean) {
        getCurrentValue().visible = visible
    }

    /**
     * Checks if this HUD should be rendered.
     * Takes into account the visible flag, module state, and dependency conditions.
     */
    fun shouldRender(): Boolean {
        return getCurrentValue().visible && shouldShowInGui()
    }

    override fun toJson(): JsonObject {
        return JsonObject().apply {
            addProperty("x", getCurrentValue().x)
            addProperty("y", getCurrentValue().y)
            addProperty("text", getCurrentValue().text)
            addProperty("visible", getCurrentValue().visible)
            addProperty("scale", getCurrentValue().scale)
        }
    }

    override fun fromJson(json: JsonElement) {
        if (json.isJsonObject) {
            val obj = json.asJsonObject
            getCurrentValue().x = obj.get("x")?.asInt ?: getCurrentValue().x
            getCurrentValue().y = obj.get("y")?.asInt ?: getCurrentValue().y
            getCurrentValue().text = obj.get("text")?.asString ?: getCurrentValue().text
            getCurrentValue().visible = obj.get("visible")?.asBoolean ?: getCurrentValue().visible
            getCurrentValue().scale = obj.get("scale")?.asFloat ?: getCurrentValue().scale
        }
    }
}
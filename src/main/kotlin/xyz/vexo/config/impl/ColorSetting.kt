package xyz.vexo.config.impl

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.awt.Color
import xyz.vexo.config.Setting

/**
 * A color setting that can be configured in a module.
 *
 * @param name The name of the setting.
 * @param description A description of the setting. Defaults to an empty string.
 * @param default The default value of the setting. Defaults to `Color.WHITE`.
 * @param allowAlpha Whether to allow alpha values in the color. Defaults to `true`.
 */
class ColorSetting(
    name: String,
    description: String = "",
    default: Color = Color.WHITE,
    val allowAlpha: Boolean = true,
) : Setting<Color>(name, description, default) {

    override fun toJson(): JsonObject {
        return JsonObject().apply {
            addProperty("r", getCurrentValue().red)
            addProperty("g", getCurrentValue().green)
            addProperty("b", getCurrentValue().blue)
            if (allowAlpha) addProperty("a", getCurrentValue().alpha)
        }
    }

    override fun fromJson(json: JsonElement) {
        if (json.isJsonObject) {
            val obj = json.asJsonObject
            val r = obj.get("r")?.asInt ?: return
            val g = obj.get("g")?.asInt ?: return
            val b = obj.get("b")?.asInt ?: return
            val a = if (allowAlpha) obj.get("a")?.asInt ?: 255 else 255
            updateValue(Color(r, g, b, a))
        }
    }

    fun getRGB(): Int = getCurrentValue().rgb
    fun getRGBA(): Int = (getCurrentValue().alpha shl 24) or (getCurrentValue().red shl 16) or (getCurrentValue().green shl 8) or getCurrentValue().blue
}
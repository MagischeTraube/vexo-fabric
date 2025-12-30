package xyz.vexo.config.impl

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import xyz.vexo.config.Setting

/**
 * A boolean setting that can be configured in a module.
 *
 * @param name The name of the setting.
 * @param description A description of the setting. Defaults to an empty string.
 * @param default The default value of the setting. Defaults to `false`.
 */
class BooleanSetting(
    name: String,
    description: String = "",
    default: Boolean = false,
) : Setting<Boolean>(name, description, default) {

    override fun toJson() = JsonPrimitive(getCurrentValue())

    override fun fromJson(json: JsonElement) {
        if (json.isJsonPrimitive && json.asJsonPrimitive.isBoolean) {
            updateValue(json.asBoolean)
        }
    }
}
package xyz.vexo.config.impl

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import xyz.vexo.config.Setting

/**
 * A selector setting that can be configured in a module.
 *
 * @param name The name of the setting.
 * @param description A description of the setting. Defaults to an empty string.
 * @param default The default value of the setting.
 * @param options A list of valid options for the setting.
 */
class SelectorSetting(
    name: String,
    description: String = "",
    default: String,
    val options: List<String>,
) : Setting<String>(name, description, default) {

    init {
        require(options.isNotEmpty()) { "Options list cannot be empty" }
        require(default in options) { "Default value must be in options" }
    }

    override fun updateValue(newValue: String) {
        if (newValue in options) {
            super.updateValue(newValue)
        }
    }

    override fun toJson(): JsonElement {
        return JsonPrimitive(getCurrentValue())
    }

    override fun fromJson(json: JsonElement) {
        if (json.isJsonPrimitive && json.asJsonPrimitive.isString) {
            updateValue(json.asString)
        }
    }
}
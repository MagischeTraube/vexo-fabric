package xyz.vexo.config.impl

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import xyz.vexo.config.Setting

class KeybindSetting(
    name: String,
    description: String = "",
    default: Int = -1
) : Setting<Int>(name, description, default) {

    var listening: Boolean = false

    override fun toJson() = JsonPrimitive(getCurrentValue())

    override fun fromJson(json: JsonElement) {
        if (json.isJsonPrimitive && json.asJsonPrimitive.isNumber) {
            updateValue(json.asInt)
        }
    }
}
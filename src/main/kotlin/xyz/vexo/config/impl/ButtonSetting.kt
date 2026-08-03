package xyz.vexo.config.impl

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import xyz.vexo.config.Setting

class ButtonSetting(
    name: String,
    description: String = "",
    val label: String = "Open",
    val onClick: () -> Unit,
) : Setting<Unit>(name, description, value = Unit) {

    override fun toJson(): JsonElement = JsonNull.INSTANCE

    override fun fromJson(json: JsonElement) = Unit
}

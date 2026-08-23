package xyz.vexo.features.impl.misc

import xyz.vexo.config.impl.SelectorSetting
import xyz.vexo.config.impl.SliderSetting
import xyz.vexo.features.Module

object TimeControl : Module(
    name = "Time Control",
    description = "Change the ingame time",
    toggled = false
) {
    private val presets = mapOf(
        "Sunrise" to 0L,
        "Day" to 1000L,
        "Noon" to 6000L,
        "Sunset" to 12000L,
        "Night" to 13000L,
        "Midnight" to 18000L
    )

    val preset = SelectorSetting(
        "Preset",
        "Time of day",
        "Custom",
        listOf("Custom") + presets.keys.toList()
    )

    val customTicks = SliderSetting(
        "Custom Ticks",
        "0–24000",
        12000.0,
        0.0,
        24000.0,
        1.0
    ).dependsOn { preset.getCurrentValue() == "Custom" }

    fun getTicks(): Long {
        val selected = preset.getCurrentValue()
        return presets[selected] ?: customTicks.getCurrentValue().toLong()
    }
}

package xyz.vexo.config.impl

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import xyz.vexo.config.Setting
import kotlin.math.pow
import kotlin.math.round

/**
 * A slider setting that can be configured in a module.
 *
 * @param name The name of the setting.
 * @param description A description of the setting. Defaults to an empty string.
 * @param default The default value of the setting. Defaults to `0.0`.
 * @param min The minimum value of the setting. Defaults to `0.0`.
 * @param max The maximum value of the setting. Defaults to `100.0`.
 * @param increment The increment value of the setting. Defaults to `0.1`.
 */
class SliderSetting(
    name: String,
    description: String = "",
    default: Double,
    val min: Double,
    val max: Double,
    val increment: Double = 0.1,
) : Setting<Double>(name, description, default) {

    override fun updateValue(newValue: Double) {
        // Runde den Wert auf die Präzision des Increments
        val roundedValue = roundToIncrement(newValue.coerceIn(min, max))
        super.updateValue(roundedValue)
    }

    override fun toJson() = JsonPrimitive(getCurrentValue())

    override fun fromJson(json: JsonElement) {
        if (json.isJsonPrimitive && json.asJsonPrimitive.isNumber) {
            updateValue(json.asDouble)
        }
    }

    /**
     * Rundet einen Wert auf die Präzision des Increments
     */
    private fun roundToIncrement(value: Double): Double {
        // Berechne die Anzahl der Dezimalstellen basierend auf dem Increment
        val decimalPlaces = getDecimalPlaces(increment)
        val multiplier = 10.0.pow(decimalPlaces)

        // Runde auf die entsprechende Anzahl Dezimalstellen
        return round(value * multiplier) / multiplier
    }

    /**
     * Bestimmt die Anzahl der Dezimalstellen des Increments
     */
    private fun getDecimalPlaces(increment: Double): Int {
        if (increment >= 1.0) return 0
        if (increment >= 0.1) return 1
        if (increment >= 0.01) return 2
        if (increment >= 0.001) return 3
        return 4 // Maximale Präzision für sehr kleine Increments
    }
}
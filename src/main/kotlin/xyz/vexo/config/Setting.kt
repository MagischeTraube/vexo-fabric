package xyz.vexo.config

import com.google.gson.JsonElement
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A base class for all settings in the Vexo config system. Each setting has a name, description,
 * current value, and optional module it belongs to. It also has a visibility flag that can be set
 * to hide a setting.
 *
 * @param name The name of the setting.
 * @param description A description of the setting. Defaults to an empty string.
 * @param value The current value of the setting.
 */
abstract class Setting<T>(
    val name: String,
    val description: String = "",
    protected var value: T,
) : ReadWriteProperty<Any?, T> {

    /**
     * Flag indicating whether the setting is visible or hidden.
     */
    var visible: Boolean = true
        private set

    /**
     * A callback function that is called when the value of the setting changes.
     */
    var onChange: ((T) -> Unit)? = null

    /**
     * Lambda that determines if this setting should be shown based on other settings.
     * Returns true by default (always show).
     */
    private var dependsOnCondition: () -> Boolean = { true }

    /**
     * Returns the current value of the setting.
     */
    fun getCurrentValue(): T = value

    /**
     * Sets the value of the setting and calls the `onChange` callback if it is not `null`.
     *
     * @param newValue The new value to set for the setting.
     */
    open fun updateValue(newValue: T) {
        value = newValue
        onChange?.invoke(newValue)
    }

    /**
     * Returns the current value of the setting.
     */
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    /**
     * Sets the value of the setting.
     */
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        updateValue(value)
    }

    /**
     * Sets the dependency condition for this setting.
     * The setting will only be visible/active when this condition returns true.
     *
     * @param condition Lambda that returns true when the setting should be shown
     * @return This setting for method chaining
     */
    fun dependsOn(condition: () -> Boolean): Setting<T> {
        this.dependsOnCondition = condition
        return this
    }

    /**
     * Checks if the setting should be shown based on its dependency condition.
     *
     * @return true if the setting should be shown, false otherwise
     */
    fun shouldShowInGui(): Boolean {
        return visible && dependsOnCondition()
    }

    /**
     * Converts the value of the setting to a JSON element.
     *
     * @return A JSON element representing the value of the setting.
     */
    abstract fun toJson(): JsonElement

    /**
     * Sets the value of the setting based on the provided JSON element.
     *
     * @param json The JSON element to update the setting from.
     */
    abstract fun fromJson(json: JsonElement)
}
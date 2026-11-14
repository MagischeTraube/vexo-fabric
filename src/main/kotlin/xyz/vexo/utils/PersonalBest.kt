package xyz.vexo.utils

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import xyz.vexo.config.ConfigManager
import xyz.vexo.config.Setting
import xyz.vexo.config.impl.StringSetting

/**
 * Stores personal best times keyed by name. Registered like any other setting (declare it as a
 * module field) so it gets persisted with the module config, but it is never shown in the GUI.
 *
 * @param name The name of the setting in the config.
 */
class PersonalBest(name: String) : Setting<MutableMap<String, Float>>(name, value = mutableMapOf()) {

    init {
        dependsOn { false }
    }

    /**
     * Compares [time] to the stored personal best for [name], saves it if it is better
     * and optionally sends a chat message with the result.
     *
     * @param name The name of the split / timer.
     * @param time The new time achieved in seconds.
     * @param message The message prefix to display.
     * @param sendMessage Whether a chat message should be sent.
     */
    fun time(name: String, time: Float, message: String, sendMessage: Boolean = true) {
        val oldPB = value[name]
        val timeText = "%.2f".format(time)

        val suffix = if (oldPB == null || time < oldPB) {
            set(name, time)
            if (oldPB == null) "§7(§d§lNew PB§r§7)"
            else "§7(§d§lNew PB§r§7) Old PB was §8${"%.2f".format(oldPB)}"
        } else "§8(§7${"%.2f".format(oldPB)}§8)"

        if (sendMessage) modMessage("$message${timeText}s§7! $suffix")
    }

    fun get(name: String): Float? = value[name]

    fun set(name: String, time: Float) {
        value[name] = time
        ConfigManager.onChanged()
    }

    fun remove(name: String) {
        if (value.remove(name) != null) ConfigManager.onChanged()
    }

    /** Copy of all stored times, e.g. to compare a run against the PBs it started with. */
    fun snapshot(): Map<String, Float> = value.toMap()

    fun reset() {
        value.clear()
        ConfigManager.onChanged()
    }

    override fun toJson() = JsonObject().apply {
        getCurrentValue().forEach { (key, time) -> addProperty(key, time) }
    }

    override fun fromJson(json: JsonElement) {
        if (!json.isJsonObject) return
        value.clear()
        json.asJsonObject.entrySet().forEach { (key, time) ->
            runCatching { value[key] = time.asFloat }
        }
    }
}

/**
 * Text field in the GUI that edits one entry of a [PersonalBest]. It stores nothing itself,
 * so the PB stays the single source of truth (runs update it, the field shows the new value).
 *
 * @param pb The personal bests to edit.
 * @param key The split name inside [pb].
 */
class PersonalBestTimeSetting(
    name: String,
    description: String,
    private val pb: PersonalBest,
    private val key: String,
) : StringSetting(name, description) {

    override fun getCurrentValue(): String = pb.get(key)?.let { formatTime((it * 1000).toLong()) } ?: ""

    /** Saves valid times right away; invalid input is ignored so the field falls back to the stored PB. */
    override fun updateValue(newValue: String) {
        if (newValue.isBlank()) pb.remove(key)
        else parseTime(newValue)?.let { pb.set(key, it) }
    }

    override fun toJson(): JsonElement = JsonNull.INSTANCE

    override fun fromJson(json: JsonElement) = Unit
}

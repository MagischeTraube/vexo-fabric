package xyz.vexo.config

import com.google.gson.JsonObject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.vexo.features.Module
import xyz.vexo.features.ModuleManager
import xyz.vexo.utils.logError
import xyz.vexo.utils.logInfo
import java.io.File
import xyz.vexo.Vexo

/**
 * Manages saving and loading of configuration data
 */
object ConfigManager {
    private val gson get() = Vexo.gson
    private var configFile = File(Vexo.configDir, "config.json")

    /** True while [load] is applying values, so change callbacks don't trigger redundant saves. */
    @Volatile
    private var loading = false
    private var pendingSave: Job? = null

    /**
     * Called whenever a setting value or module toggle changes. Schedules a debounced save so
     * changes persist immediately instead of relying solely on the config GUI being closed.
     */
    fun onChanged() {
        if (loading) return
        pendingSave?.cancel()
        pendingSave = Vexo.scope.launch {
            delay(3500)
            save()
        }
    }

    /**
     * Saves all modules and their settings to JSON
     */
    fun save() {
        try {
            val root = JsonObject()

            val modulesJson = JsonObject()
            ModuleManager.getAllModules().forEach { module ->
                val moduleJson = JsonObject()

                moduleJson.addProperty("enabled", module.enabled)

                val settingsJson = JsonObject()
                getSettingsFromModule(module).forEach { setting ->
                    // Isolate each setting so a single bad toJson() can't abort the whole save
                    // (which would otherwise leave the file un-updated).
                    runCatching { settingsJson.add(setting.name, setting.toJson()) }
                        .onFailure { logError(Exception("Failed to serialize setting '${module.name}/${setting.name}'", it), this) }
                }
                moduleJson.add("settings", settingsJson)

                modulesJson.add(module.name, moduleJson)
            }
            root.add("modules", modulesJson)

            configFile.writeText(gson.toJson(root))
            logInfo("Config saved successfully")
        } catch (e: Exception) {
            logError(e, this)
        }
    }

    /**
     * Loads all modules and their settings from JSON
     */
    fun load() {
        try {
            if (!configFile.exists()) {
                logInfo("No config file found, using defaults")
                return
            }

            loading = true
            val root = gson.fromJson(configFile.readText(), JsonObject::class.java)
            val modulesJson = root.getAsJsonObject("modules") ?: return

            ModuleManager.getAllModules().forEach { module ->
                // Isolate each module so one module's malformed entry can't abort loading of the
                // rest (modules registered later — e.g. Wardrobe — would otherwise silently keep
                // their defaults and get overwritten on the next save).
                runCatching {
                    val moduleJson = modulesJson.getAsJsonObject(module.name) ?: return@runCatching

                    val enabled = moduleJson.get("enabled")?.asBoolean ?: false
                    if (enabled != module.enabled) {
                        module.toggle()
                    }

                    val settingsJson = moduleJson.getAsJsonObject("settings") ?: return@runCatching
                    getSettingsFromModule(module).forEach { setting ->
                        // Isolate each setting too, for the same reason within a module.
                        runCatching {
                            settingsJson.get(setting.name)?.let { json -> setting.fromJson(json) }
                        }.onFailure { logError(Exception("Failed to load setting '${module.name}/${setting.name}'", it), this) }
                    }
                }.onFailure { logError(Exception("Failed to load module '${module.name}'", it), this) }
            }

            logInfo("Config loaded successfully")
        } catch (e: Exception) {
            logError(e, this)
        } finally {
            loading = false
        }
    }

    /**
     * Retrieves all settings from a given module.
     * @param module The module to extract settings from.
     * @return A list of settings from the module.
     */
    fun getSettingsFromModule(module: Module): List<Setting<*>> =
        module.settings
}
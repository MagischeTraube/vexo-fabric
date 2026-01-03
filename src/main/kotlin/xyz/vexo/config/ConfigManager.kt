package xyz.vexo.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.fabricmc.loader.api.FabricLoader
import xyz.vexo.Vexo.MOD_ID
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
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private var configFile = File(Vexo.configDir, "config.json")

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
                    settingsJson.add(setting.name, setting.toJson())
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

            val root = gson.fromJson(configFile.readText(), JsonObject::class.java)
            val modulesJson = root.getAsJsonObject("modules") ?: return

            ModuleManager.getAllModules().forEach { module ->
                val moduleJson = modulesJson.getAsJsonObject(module.name) ?: return@forEach

                val enabled = moduleJson.get("enabled")?.asBoolean ?: false
                if (enabled != module.enabled) {
                    module.toggle()
                }

                val settingsJson = moduleJson.getAsJsonObject("settings") ?: return@forEach
                getSettingsFromModule(module).forEach { setting ->
                    settingsJson.get(setting.name)?.let { json ->
                        setting.fromJson(json)
                    }
                }
            }

            logInfo("Config loaded successfully")
        } catch (e: Exception) {
            logError(e, this)
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
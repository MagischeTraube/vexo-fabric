package xyz.vexo.features

/**
 * Manages all modules in the mod.
 * Provides functionality to register, retrieve, and manage modules.
 */
object ModuleManager {
    private val modules = mutableListOf<Module>()

    fun register(module: Module) {
        module.initSettings()
        modules.add(module)
    }

    fun getModule(name: String): Module? =
        modules.find { it.name.equals(name, ignoreCase = true) }

    fun getModulesByCategory(category: Category): List<Module> =
        modules.filter { it.category == category }

    fun getAllModules(): List<Module> = modules

    fun getEnabledModules(): List<Module> = modules.filter { it.enabled }
}
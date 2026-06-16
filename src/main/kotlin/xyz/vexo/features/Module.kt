package xyz.vexo.features

import kotlin.reflect.KProperty1
import xyz.vexo.events.EventBus
import xyz.vexo.config.ConfigManager
import xyz.vexo.config.Setting

/**
 * Base class for all modules in the mod.
 * Provides core functionality for module management, settings, and event handling.
 *
 * @param name The name of the module.
 * @param description The description of the module.
 * @param toggled Whether the module is enabled by default.
 */
abstract class Module(
    val name: String,
    val description: String = "",
    toggled: Boolean = false
) {
    val category: Category = Category.fromPackage(this::class.java) ?: Category.MISC
    val settings = mutableListOf<Setting<*>>()

    var enabled: Boolean = toggled
        private set

    open fun onEnable() {
        EventBus.subscribe(this)
    }

    open fun onDisable() {
        EventBus.unsubscribe(this)
    }

    fun toggle() {
        enabled = !enabled
        if (enabled) onEnable() else onDisable()
        ConfigManager.onChanged()
    }

    fun initSettings() {
        if (settings.isNotEmpty()) return

        val fields = this::class.java.declaredFields

        val memberMap = this::class.members
            .filterIsInstance<KProperty1<Module, *>>()
            .associateBy { it.name }

        fields.forEach { field ->
            field.isAccessible = true
            val value = runCatching { field.get(this) }.getOrNull()
            if (value is Setting<*>) settings += value
            else {
                val propName = field.name.removeSuffix("\$delegate")

                val prop = memberMap[propName]
                val delegate = prop?.getDelegate(this)
                if (delegate is Setting<*> && delegate !in settings) {
                    settings += delegate
                }
            }
        }
    }
}
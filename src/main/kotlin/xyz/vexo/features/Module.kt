package xyz.vexo.features

import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.isAccessible
import xyz.vexo.events.EventBus
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
    }

    fun initSettings() {
        if (settings.isNotEmpty()) return

        this::class.members
            .filterIsInstance<KProperty1<Module, *>>()
            .forEach { prop ->
                prop.isAccessible = true
                val delegate = prop.getDelegate(this)
                if (delegate is Setting<*> && delegate !in settings) {
                    settings += delegate
                }
            }
    }
}

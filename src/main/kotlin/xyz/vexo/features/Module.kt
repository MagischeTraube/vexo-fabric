package xyz.vexo.features

import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.isAccessible
import xyz.vexo.events.EventBus
import xyz.vexo.config.Setting

/**
 * Base class for all modules in the mod.
 * Provides core functionality for module management, settings, and event handling.
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

    val alwaysActive: Boolean = this::class.hasAnnotation<AlwaysActive>()

    init {
        if (alwaysActive) {
            EventBus.subscribe(this)
        }
    }

    open fun onEnable() {
        if (!alwaysActive) EventBus.subscribe(this)
    }

    open fun onDisable() {
        if (!alwaysActive) EventBus.unsubscribe(this)
    }

    fun toggle() {
        enabled = !enabled
        if (enabled) onEnable() else onDisable()
    }

    fun initSettings() {
        if (settings.isNotEmpty()) return

        val fields = this::class.java.declaredFields

        fields.forEach { field ->
            field.isAccessible = true

            try {
                val value = field.get(this)
                if (value is Setting<*>) {
                    settings += value
                }
            } catch (e: Exception) {
                try {
                    val kotlinProp = this::class.members
                        .filterIsInstance<KProperty1<Module, *>>()
                        .find { it.name == field.name.removeSuffix("\$delegate") }

                    if (kotlinProp != null) {
                        kotlinProp.isAccessible = true
                        val delegate = kotlinProp.getDelegate(this)
                        if (delegate is Setting<*> && delegate !in settings) {
                            settings += delegate
                        }
                    }
                } catch (ignored: Exception) {}
            }
        }
    }
}

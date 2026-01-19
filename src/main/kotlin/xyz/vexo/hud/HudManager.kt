package xyz.vexo.hud

import net.minecraft.client.gui.GuiGraphics
import xyz.vexo.config.impl.HudSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.HudRenderEvent
import xyz.vexo.features.ModuleManager
import xyz.vexo.Vexo.mc
import xyz.vexo.events.EventBus
import xyz.vexo.utils.renderString


/**
 * Manager for handling all HUD elements in the client.
 * Automatically renders all active HUDs that meet their visibility conditions.
 */
object HudManager {
    private val registeredHuds = mutableListOf<HudSetting>()
    private val visibleHuds = mutableListOf<HudSetting>()

    init {
        EventBus.subscribe(this)
    }

    /**
     * Registers a HUD setting to be managed by this manager.
     * Called automatically when a HudSetting is created.
     */
    fun registerHud(hudSetting: HudSetting) {
        if (hudSetting !in registeredHuds) {
            registeredHuds.add(hudSetting)
        }
    }

    /**
     * Updates the visibility of a specific HUD setting.
     * Called when a module's enabled state or dependency conditions change.
     *
     * @param hudSetting The HUD setting to update
     */
    fun updateHudVisibility(hudSetting: HudSetting) {
        val module = ModuleManager.getAllModules().find {
            it.settings.contains(hudSetting)
        }

        val shouldBeVisible = module?.enabled == true && hudSetting.shouldRender()

        if (shouldBeVisible) {
            if (hudSetting !in visibleHuds) {
                visibleHuds.add(hudSetting)
            }
        } else {
            visibleHuds.remove(hudSetting)
        }
    }

    /**
     * Event handler that renders all active HUDs.
     * This is automatically called via the EventBus when RenderEvent is posted.
     */
    @EventHandler
    fun onRender(event: HudRenderEvent) {
        if (mc.screen is MoveActiveHudsGui) return
        renderHuds(event.context)
    }

    /**
     * Renders all visible HUDs.
     *
     * @param context The drawing context
     */
    private fun renderHuds(context: GuiGraphics) {
        visibleHuds.forEach { hudSetting ->
            val hudElement = hudSetting.getCurrentValue()
            if (hudElement.text.isNotEmpty()) {
                renderHud(context, hudElement)
            }
        }
    }

    /**
     * Renders a single HUD element with proper scaling.
     *
     * @param context The drawing context
     * @param hud The HUD element to render
     */
    private fun renderHud(context: GuiGraphics, hud: HudElement) {
        renderString(context, hud.text, hud.x.toFloat(), hud.y.toFloat(), hud.scale)
    }

    /**
     * Gets all HUDs that should be shown in the MoveActiveHudsGui.
     * Returns HUDs where the module is enabled and dependsOn conditions are met.
     */
    fun getActiveHuds(): List<HudSetting> {
        return registeredHuds.filter { hudSetting ->
            val module = ModuleManager.getAllModules().find { module ->
                module.settings.contains(hudSetting)
            }

            module?.enabled == true && hudSetting.shouldShowInGui()
        }
    }

    /**
     * Gets all registered HUDs
     */
    fun getAllHuds(): List<HudSetting> = registeredHuds.toList()

}
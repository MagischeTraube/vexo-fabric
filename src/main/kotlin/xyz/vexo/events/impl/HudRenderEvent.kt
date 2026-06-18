package xyz.vexo.events.impl


import net.minecraft.client.gui.GuiGraphicsExtractor
import xyz.vexo.events.Event

/**
 * Event that is fired when the HUD is being rendered.
 * Use this to render custom HUD elements.
 *
 * @param context The GUI graphics context
 */
class HudRenderEvent(
    val context: GuiGraphicsExtractor
) : Event()
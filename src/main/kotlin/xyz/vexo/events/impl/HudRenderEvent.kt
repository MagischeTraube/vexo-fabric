package xyz.vexo.events.impl


import net.minecraft.client.gui.GuiGraphics
import xyz.vexo.events.Event

/**
 * Event that is posted when the HUD is being rendered.
 * Use this to render custom HUD elements.
 */
class HudRenderEvent(
    val context: GuiGraphics
) : Event()
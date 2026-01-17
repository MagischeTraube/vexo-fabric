package xyz.vexo.events.impl

import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.GuiGraphics
import xyz.vexo.events.Event

/**
 * Event fired when a GUI is being rendered.
 *
 * @param screen The screen being rendered
 * @param context The GUI graphics context
 * @param mouseX The mouse X position
 * @param mouseY The mouse Y position
 */
class GuiRenderEvent(
    val screen: Screen,
    val context: GuiGraphics,
    val mouseX: Int,
    val mouseY: Int,
) : Event()
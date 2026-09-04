package xyz.vexo.events.impl

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.inventory.Slot
import xyz.vexo.events.CancellableEvent

/**
 * Event fired when slots are being rendered in a GUI.
 * Allows modules to post slot highlight colors that will be rendered on top of the GUI.
 *
 * @param screen The screen being rendered
 * @param context The GUI graphics context
 * @param slot The slot being rendered
 */
class SlotGuiRenderEvent(
    val screen: Screen,
    val context: GuiGraphicsExtractor,
    val slot: Slot
) : CancellableEvent()

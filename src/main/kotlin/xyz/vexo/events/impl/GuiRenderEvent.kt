package xyz.vexo.events.impl

import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.GuiGraphics
import xyz.vexo.events.Event

class GuiRenderEvent(
    val screen: Screen,
    val context: GuiGraphics,
    val mouseX: Int,
    val mouseY: Int,
) : Event()
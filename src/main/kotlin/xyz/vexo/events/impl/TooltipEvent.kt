package xyz.vexo.events.impl

import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import xyz.vexo.events.Event

class TooltipEvent(
    val screen: Screen,
    val itemStack: ItemStack,
    @JvmField val lines: MutableList<Component>
) : Event()
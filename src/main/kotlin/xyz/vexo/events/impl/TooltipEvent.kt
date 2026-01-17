package xyz.vexo.events.impl

import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import xyz.vexo.events.Event

/**
 * Event fired when an item tooltip is being rendered.
 *
 * @param screen The screen where the tooltip is being rendered
 * @param itemStack The item stack being rendered
 * @param lines The lines of the tooltip
 */
class TooltipEvent(
    val screen: Screen,
    val itemStack: ItemStack,
    @JvmField val lines: MutableList<Component>
) : Event()
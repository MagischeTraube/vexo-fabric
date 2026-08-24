package xyz.vexo.events.impl

import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ContainerInput
import xyz.vexo.events.CancellableEvent

class ContainerSlotClickEvent(
    val containerId: Int,
    val slotId: Int,
    val button: Int,
    val input: ContainerInput,
    val player: Player
) : CancellableEvent() {
    companion object {
        @JvmStatic
        var bypass: Boolean = false
    }
}

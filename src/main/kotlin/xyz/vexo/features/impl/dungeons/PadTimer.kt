package xyz.vexo.features.impl.dungeons

import xyz.vexo.config.impl.SelectorSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessagePacketEvent
import xyz.vexo.events.impl.ServerTickEvent
import xyz.vexo.features.Module
import xyz.vexo.utils.modMessage

object PadTimer : Module(
    name = "Pad Timer",
    description = "Timer for when to crush Storm",
    toggled = false
) {
    private val crushOrder by SelectorSetting(
        name = "Crush Order",
        description = "In what order you crush storm",
        default = "Green-Yellow",
        options = listOf("Green-Yellow", "Purple-Yellow")
    )
    private var serverTicks = 0

    private val PadTimer = listOf(
        Regex("\\[BOSS] Storm: ENERGY HEED MY CALL!"),
        Regex("\\[BOSS] Storm: THUNDER LET ME BE YOUR CATALYST!")
    )

    @EventHandler
    fun onChat(event: ChatMessagePacketEvent) {
        if (PadTimer.any { it.containsMatchIn(event.message) })
            when (crushOrder) {
                "Green-Yellow" -> serverTicks = 181
                "Purple-Yellow" -> serverTicks = 106
            }
    }

    @EventHandler
    fun onServerTick(event: ServerTickEvent) {
        if (serverTicks != 0)
            serverTicks --

        when(serverTicks){
            60 -> modMessage("Pad in §a2.5s!")
            50 -> modMessage("Pad in §a2.0s!")
            40 -> modMessage("Pad in §a1.5s!")
            30 -> modMessage("Pad in §e1.0s!")
            20 -> modMessage("Pad in §e0.5s!")
            10 -> modMessage("Pad §cNOW!")
        }
    }
}
package xyz.vexo.features.impl.dungeons

import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessagePacketEvent
import xyz.vexo.events.impl.ServerTickEvent
import xyz.vexo.events.impl.WorldJoinEvent
import xyz.vexo.features.Module
import xyz.vexo.utils.modMessage

object PadTimer : Module(
    name = "Pad Timer",
    description = "Timer for when to crush Storm",
    toggled = false
) {
    private var padTimerTriggered = false
    private var serverTicks = 0

    private val padTimer = listOf(
        Regex("\\[BOSS] Storm: ENERGY HEED MY CALL!"),
        Regex("\\[BOSS] Storm: THUNDER LET ME BE YOUR CATALYST!")
    )

    @EventHandler
    fun onChat(event: ChatMessagePacketEvent) {
        if (!padTimerTriggered && padTimer.any { it.containsMatchIn(event.message) }) {
            padTimerTriggered = true
            serverTicks = 55
        }
    }

    private val alerts = mapOf(
        60 to "Pad in §a2.5s!",
        50 to "Pad in §a2.0s!",
        40 to "Pad in §a1.5s!",
        30 to "Pad in §e1.0s!",
        20 to "Pad in §e0.5s!",
        10 to "Pad §cNOW!"
    )

    @EventHandler
    fun onServerTick(event: ServerTickEvent) {
        if (serverTicks != 0)
            serverTicks--

        alerts[serverTicks]?.let { modMessage(it) }
    }

    @EventHandler
    fun onWorldJoin(event: WorldJoinEvent) {
        padTimerTriggered = false
        serverTicks = 0
    }
}

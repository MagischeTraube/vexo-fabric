package xyz.vexo.features.impl.kuudra

import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessagePacketEvent
import xyz.vexo.events.impl.RunEndEvent
import xyz.vexo.features.Module
import xyz.vexo.utils.modMessage
import xyz.vexo.utils.sendCommand

object SoloDetector : Module(
    name = "Solo Detector",
    description = "Alerts your party when all other teammates are eliminated in Kuudra",
    toggled = false
) {
    private var killCount = 0
    private var messageSent = false
    private val finalKilledRegex = Regex("([A-Za-z0-9]+) was FINAL KILLED by Kuudra!$")

    @EventHandler
    fun onChat(event: ChatMessagePacketEvent) {
        if (killCount == 3 && !messageSent) {
            modMessage("solo")
            sendCommand("pc SOLO")
            messageSent = true
            return
        }

        if (finalKilledRegex.containsMatchIn(event.unformattedMessage) && !messageSent) {
            killCount++
            return
        }
    }

    @EventHandler
    fun runEnd(event: RunEndEvent) {
        messageSent = false
        killCount = 0
    }
}

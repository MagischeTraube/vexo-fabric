package xyz.vexo.features.impl.misc

import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessagePacketEvent
import xyz.vexo.events.impl.WorldJoinEvent
import xyz.vexo.features.Module
import xyz.vexo.utils.modMessage
import xyz.vexo.utils.removeFormatting
import xyz.vexo.utils.runAfterServerTicks
import xyz.vexo.utils.sendCommand

object AutoRejoin : Module(
    name = "Auto Rejoin",
    description = "Automatically rejoins Hypixel SkyBlock after being Kicked",
    toggled = false
) {
    var rejoining = false
    private val kickedMessage = listOf(
        Regex("You were kicked while joining that server!")
    )

    @EventHandler
    fun onChat(event: ChatMessagePacketEvent) {

        val cleanMessage = event.message.removeFormatting()

        if (rejoining && cleanMessage == "Welcome to Hypixel SkyBlock!") {
            rejoining = false
            return
        }


        if (kickedMessage.any { it.containsMatchIn(cleanMessage) } && !rejoining){
            rejoining = true

            modMessage("Kicked from SkyBlock, rejoining automatically in 65 Seconds!")
            runAfterServerTicks(700) {
                modMessage("Rejoining in 30 Seconds!")
                runAfterServerTicks(600) {
                    modMessage("Rejoining Now!")
                    sendCommand("play skyblock")
                }
            }
        }
    }
    @EventHandler
    fun worldJoin(event: WorldJoinEvent){
        if (!rejoining) return

        rejoining = false
        runAfterServerTicks(5){
            modMessage("Auto rejoin has been disabled due to leaving the Hypixel Lobby.")
        }
    }
}
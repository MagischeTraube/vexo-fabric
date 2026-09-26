package xyz.vexo.features.impl.misc

import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessagePacketEvent
import xyz.vexo.events.impl.WorldJoinEvent
import xyz.vexo.features.Module
import xyz.vexo.utils.ScoreboardUtils
import xyz.vexo.utils.TablistUtils
import xyz.vexo.utils.modMessage
import xyz.vexo.utils.runAfterClientTicks
import xyz.vexo.utils.runAfterServerTicks
import xyz.vexo.utils.sendCommand

object AutoRejoin : Module(
    name = "Auto Rejoin",
    description = "Automatically rejoins Hypixel SkyBlock after being Kicked",
    toggled = false
) {
    var rejoining = false
    private var waitingForPrototype = false
    private val kickedMessage = listOf(
        Regex("You were kicked while joining that server!")
    )
    private val prototypeMarker = Regex("Prototype", RegexOption.IGNORE_CASE)

    private fun onPrototype(): Boolean =
        prototypeMarker.containsMatchIn(ScoreboardUtils.getTitle() ?: "") ||
            ScoreboardUtils.find(prototypeMarker) != null ||
            TablistUtils.find(prototypeMarker) != null

    @EventHandler
    fun onChat(event: ChatMessagePacketEvent) {
        if (rejoining && event.unformattedMessage == "Welcome to Hypixel SkyBlock!") {
            runAfterServerTicks(100){
                rejoining = false
            }
            return
        }


        if (kickedMessage.any { it.containsMatchIn(event.unformattedMessage) }){
            if (rejoining) return
            rejoining = true
            waitingForPrototype = true

            modMessage("Kicked from SkyBlock, waiting until back on Prototype...")
            waitForPrototypeThenRejoin()
        }
    }

    private fun waitForPrototypeThenRejoin() {
        if (!rejoining || !waitingForPrototype) return
        if (!enabled) {
            rejoining = false
            waitingForPrototype = false
            return
        }

        if (!onPrototype()) {
            runAfterClientTicks(20) { waitForPrototypeThenRejoin() }
            return
        }

        waitingForPrototype = false
        modMessage("Back on Prototype, rejoining automatically in 60 Seconds!")
        runAfterServerTicks(600) {
            modMessage("Rejoining in 30 Seconds!")
        }
        runAfterServerTicks(1300) {
            modMessage("Rejoining Now!")
            rejoining = false
            sendCommand("play skyblock")
        }
    }

    @EventHandler
    fun worldJoin(event: WorldJoinEvent){
        if (!rejoining || waitingForPrototype) return

        rejoining = false
        runAfterServerTicks(5){
            modMessage("Auto rejoin has been disabled due to leaving the Hypixel Lobby.")
        }
    }
}
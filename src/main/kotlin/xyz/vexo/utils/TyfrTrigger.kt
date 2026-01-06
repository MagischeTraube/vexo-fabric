package xyz.vexo.utils

import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessagePacketEvent
import xyz.vexo.events.impl.WorldJoinEvent

object TyfrTrigger {
    var tyfrToggle = false
    val tyfrMessages = listOf(
        Regex("Score:"),
        Regex("Tokens Earned:")
    )

    @EventHandler
    fun onChat(event: ChatMessagePacketEvent) {
        if (tyfrToggle && tyfrMessages.any {it.containsMatchIn(event.message.removeFormatting())}) {
            sendCommand("p leave")
            runAfterServerTicks(5) {
                sendCommand("ac tyfr o/")
            }
        }
    }

    @EventHandler
    fun onWorldJoin(event: WorldJoinEvent){
        tyfrToggle = false
    }
}
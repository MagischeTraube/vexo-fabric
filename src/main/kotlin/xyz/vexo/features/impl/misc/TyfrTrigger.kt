package xyz.vexo.features.impl.misc

import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessagePacketEvent
import xyz.vexo.events.impl.WorldJoinEvent
import xyz.vexo.features.Module
import xyz.vexo.utils.removeFormatting
import xyz.vexo.utils.sendCommand
import xyz.vexo.utils.runAfterServerTicks

object TyfrTrigger : Module(
    name = "Thank you for run",
    description = "Leaves the party after the run has ended"
) {
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
    fun worldLeave(event: WorldJoinEvent){
        tyfrToggle = false
    }
}
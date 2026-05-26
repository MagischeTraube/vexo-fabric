package xyz.vexo.utils

import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.RunEndEvent
import xyz.vexo.events.impl.WorldJoinEvent

object TyfrTrigger {
    var tyfrToggle = false

    @EventHandler
    fun onRunEnd(event: RunEndEvent) {
        if (!tyfrToggle) return
        sendCommand("p leave")
        runAfterServerTicks(4) {
            sendCommand("ac tyfr o/")
        }
    }

    @EventHandler
    fun onWorldJoin(event: WorldJoinEvent){
        tyfrToggle = false
    }
}
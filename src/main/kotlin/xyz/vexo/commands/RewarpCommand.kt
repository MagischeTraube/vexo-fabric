package xyz.vexo.commands

import com.github.stivais.commodore.Commodore
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.WorldJoinEvent
import xyz.vexo.utils.runAfterServerTicks
import xyz.vexo.utils.sendCommand

var rewarping = false
var serverTicks = 0
var warping = false
var warpName = ""

val RewarpCommand = Commodore("rewarp") {
    runs { islandName: String ->
        rewarp(islandName)
    }
}

private fun rewarp(name: String) {
    sendCommand("is")
    rewarping = true
    warpName = name
}

@EventHandler
fun worldJoin(@Suppress("UNUSED_PARAMETER") event: WorldJoinEvent) {
    if (!rewarping) return
    runAfterServerTicks(80) {
        sendCommand("warp $warpName")
    }
}
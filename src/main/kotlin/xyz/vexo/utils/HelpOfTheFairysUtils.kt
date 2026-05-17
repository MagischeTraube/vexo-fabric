package xyz.vexo.utils

import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessagePacketEvent
import xyz.vexo.events.impl.ServerTickEvent
import xyz.vexo.utils.DungeonUtils
import xyz.vexo.Vexo.mc
import java.util.LinkedList

/**
 * Help of the Fairys will be renamed to HOF
 */
object HelpOfTheFairysUtils {

    val revivedPlayers: LinkedList<String> = LinkedList()

    private var tickCounter = 0

    private fun reviveRegex(player: String) = Regex("\\s*❣ (.+) was revived by ${Regex.escape(player)}!")

    @EventHandler
    fun onServerTick(event: ServerTickEvent) {
        if (++tickCounter % 20 != 0) return
        DevMode.ifFlag("printDeathList") {
            if (revivedPlayers.isEmpty()) modMessage("HOF revived: none")
            else revivedPlayers.forEachIndexed { i, name -> modMessage("HOF [$i] $name") }
        }
    }

    @EventHandler
    fun sortHOF(event: ChatMessagePacketEvent){
        DevMode.ifFlag("debugHOF") {
            if (event.unformattedMessage.contains("revived")) modMessage("HOF msg: '${event.unformattedMessage}'")
        }

        if (!DungeonUtils.inDungeon) return

        val localPlayer = mc.player?.gameProfile?.name ?: return

        reviveRegex(localPlayer).find(event.unformattedMessage)?.groupValues?.get(1)?.let {
            revivedPlayers.add(it)
        }
    }
}


/*
 ❣ MagischeTraube was revived by InfernoLloyd!
  ❣ InfernoLloyd was revived by MagischeTraube!
 */
//
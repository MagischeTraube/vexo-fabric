package xyz.vexo.commands

import com.github.stivais.commodore.Commodore
import xyz.vexo.utils.PartyUtils.partyLeader
import xyz.vexo.utils.runAfterServerTicks
import xyz.vexo.utils.sendCommand

val reInviteCommand = Commodore("reinvite") {
    runs {
        val partyLead = partyLeader
        sendCommand("p leave")
        runAfterServerTicks(10){
            sendCommand("msg $partyLead !inv")
        }
    }
}
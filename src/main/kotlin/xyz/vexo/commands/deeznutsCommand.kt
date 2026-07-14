package xyz.vexo.commands

import com.github.stivais.commodore.Commodore
import xyz.vexo.utils.modMessage
import xyz.vexo.utils.sendCommand

val deezNutsCommand = Commodore("dn") {
    runs {
        sendCommand("warp dungeon_hub")
        modMessage("Warping to: Deez Nuts lmao...")
    }
}
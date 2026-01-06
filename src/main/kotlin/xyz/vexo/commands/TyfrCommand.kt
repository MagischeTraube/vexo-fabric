package xyz.vexo.commands

import com.github.stivais.commodore.Commodore
import xyz.vexo.utils.TyfrTrigger
import xyz.vexo.utils.modMessage

val TyfrCommand = Commodore("tyfr") {
    runs {
        TyfrTrigger.tyfrToggle = !TyfrTrigger.tyfrToggle

        if (TyfrTrigger.tyfrToggle) {
            modMessage("§aTYFR activated! §r– waiting for the end of the run!")
        } else {
            modMessage("§cTYFR deactivated!")
        }

    }
}
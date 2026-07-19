package xyz.vexo.commands

import com.github.stivais.commodore.Commodore
import xyz.vexo.Vexo
import xyz.vexo.utils.showKuudraStats

val KuudraStatsCommand = Commodore("kuudrastats") {

    runs { showKuudraStats(Vexo.mc.user.name) }

    runs { playerName: String ->
        showKuudraStats(playerName)
    }
}
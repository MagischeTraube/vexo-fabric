package xyz.vexo.commands

import com.github.stivais.commodore.Commodore
import xyz.vexo.Vexo
import xyz.vexo.utils.showKuudraStats

val KuudraStatsCommand = Commodore("kuudrastats") {
    // /kuudrastats          -> your own stats
    runs {
        showKuudraStats(Vexo.mc.user.name)
    }
    // /kuudrastats <name>   -> someone else's stats
    runs { playerName: String ->
        showKuudraStats(playerName)
    }
}
package xyz.vexo.commands

import com.github.stivais.commodore.Commodore
import xyz.vexo.clickgui.ClickGui
import xyz.vexo.utils.runAfterClientTicks
import xyz.vexo.hud.MoveActiveHudsGui
import xyz.vexo.utils.DevMode
import gg.essential.universal.UScreen


fun devToggle(flag: String) = DevMode.toggle(flag)

val VexoCommand = Commodore("vexo") {
    runs {
        runAfterClientTicks(1) {
            UScreen.displayScreen(ClickGui())
        }
    }

    literal("move").runs {
        runAfterClientTicks(1) {
            UScreen.displayScreen(MoveActiveHudsGui())
        }
    }

    literal("dev").executable {
        runs(::devToggle)
    }

}
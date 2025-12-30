package xyz.vexo.commands

import com.github.stivais.commodore.Commodore
import xyz.vexo.clickgui.ClickGui
import xyz.vexo.utils.runAfterClientTicks
import xyz.vexo.hud.MoveActiveHudsGui
import gg.essential.universal.UScreen


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

}
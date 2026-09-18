package xyz.vexo.commands

import com.github.stivais.commodore.Commodore
import gg.essential.universal.UScreen
import xyz.vexo.features.impl.misc.CustomRecipeGui
import xyz.vexo.features.impl.misc.recipe.RecipeGUI
import xyz.vexo.utils.runAfterClientTicks
import xyz.vexo.utils.sendRawCommandToServer

val RecipeCommand = Commodore("recipe") {
    runs {
        if (!CustomRecipeGui.enabled) {
            sendRawCommandToServer("recipe")
            return@runs
        }
        runAfterClientTicks(1) {
            UScreen.displayScreen(RecipeGUI())
        }
    }

    runs { itemName: String ->
        if (!CustomRecipeGui.enabled) {
            sendRawCommandToServer("recipe $itemName")
            return@runs
        }
        runAfterClientTicks(1) {
            UScreen.displayScreen(RecipeGUI(itemName))
        }
    }
}

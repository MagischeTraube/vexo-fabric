package xyz.vexo.commands

import com.github.stivais.commodore.Commodore
import gg.essential.universal.UScreen

import xyz.vexo.features.impl.misc.recipe.RecipeGUI
import xyz.vexo.utils.runAfterClientTicks

val RecipeCommand = Commodore("recipe") {
    runs {
        runAfterClientTicks(1) {
            UScreen.displayScreen(RecipeGUI())
        }
    }

    runs { itemName: String ->
        runAfterClientTicks(1) {
            UScreen.displayScreen(RecipeGUI(itemName))
        }
    }
}
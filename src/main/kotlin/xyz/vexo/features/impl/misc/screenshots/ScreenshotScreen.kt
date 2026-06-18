package xyz.vexo.features.impl.misc.screenshots

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import xyz.vexo.Vexo.mc

class ScreenshotScreen(private val previousScreen: Screen?) : Screen(Component.literal("Screenshots")) {
    override fun extractBackground(
        guiGraphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float
    ) {

    }

    override fun isPauseScreen() = false

    override fun onClose() {
        mc.setScreen(previousScreen)
    }
}
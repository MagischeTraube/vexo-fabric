package xyz.vexo.features.impl.misc.screenshots.ui.components

import net.minecraft.client.gui.GuiGraphicsExtractor
import xyz.vexo.features.impl.misc.screenshots.config.ScreenshotColors
import xyz.vexo.features.impl.misc.screenshots.ScreenshotHud
import xyz.vexo.features.impl.misc.screenshots.ScreenshotMode
import xyz.vexo.features.impl.misc.screenshots.config.ButtonConfig
import xyz.vexo.features.impl.misc.screenshots.config.ColorPickerConfig
import xyz.vexo.features.impl.misc.screenshots.util.SelectionBounds

object ColorPickerRenderer {
    /**
     * Renders the color picker.
     *
     * @param graphics The graphics object to render to.
     * @param selection The selection bounds.
     * @param currentMode The current mode.
     */
    fun render(graphics: GuiGraphicsExtractor, selection: SelectionBounds, currentMode: ScreenshotMode) {
        val size = ColorPickerConfig.SIZE
        val gap = ColorPickerConfig.GAP

        val left = selection.left.toInt()
        val right = selection.right.toInt()
        val top = selection.top.toInt()
        val bottom = selection.bottom.toInt()

        val buttonsBaseX = ButtonPositionManager.getBaseX(left, right)
        val buttonsBaseY = ButtonPositionManager.getBaseY(top, bottom, currentMode == ScreenshotMode.DRAW)

        val startX = buttonsBaseX + (ButtonConfig.TOTAL_WIDTH / 2) - (ColorPickerConfig.TOTAL_WIDTH / 2)
        val y = buttonsBaseY + ButtonConfig.HEIGHT + ColorPickerConfig.Y_OFFSET_BELOW_BUTTONS

        for (i in ScreenshotColors.drawColors.indices) {
            val color = ScreenshotColors.drawColors[i]
            val x = startX + (i * (size + gap))

            if (color == ScreenshotHud.selectedDrawColor) {
                graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, ScreenshotColors.handleFill)
            } else {
                graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, ScreenshotColors.handleBorder)
            }
            graphics.fill(x, y, x + size, y + size, color.rgb)
        }
    }
}
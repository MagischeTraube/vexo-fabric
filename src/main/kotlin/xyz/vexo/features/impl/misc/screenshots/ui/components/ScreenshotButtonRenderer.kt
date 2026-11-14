package xyz.vexo.features.impl.misc.screenshots.ui.components

import net.minecraft.client.gui.GuiGraphicsExtractor
import xyz.vexo.Vexo
import xyz.vexo.features.impl.misc.screenshots.config.ScreenshotColors
import xyz.vexo.features.impl.misc.screenshots.ScreenshotMode
import xyz.vexo.features.impl.misc.screenshots.util.SelectionBounds

object ScreenshotButtonRenderer {
    /**
     * Renders all screenshot buttons based on the current selection and mode.
     *
     * @param graphics The graphics object to render with.
     * @param mouseX The current mouse X position.
     * @param mouseY The current mouse Y position.
     * @param selection The current selection bounds.
     * @param currentMode The current screenshot mode.
     */
    fun render(
        graphics: GuiGraphicsExtractor,
        mouseX: Double,
        mouseY: Double,
        selection: SelectionBounds,
        currentMode: ScreenshotMode
    ) {
        val left = selection.left.toInt()
        val right = selection.right.toInt()
        val top = selection.top.toInt()
        val bottom = selection.bottom.toInt()

        val baseX = ButtonPositionManager.getBaseX(left, right)
        val baseY = ButtonPositionManager.getBaseY(top, bottom, currentMode == ScreenshotMode.DRAW)

        for (type in ScreenshotButtonType.entries) {
            val layout = ScreenshotButtonLayouts.get(type, baseX, baseY)
            val hovered = layout.contains(mouseX, mouseY) ||
                    (type == ScreenshotButtonType.MOVE && ButtonPositionManager.isDragging())

            drawButton(
                graphics,
                layout,
                ScreenshotButtonLayouts.label(type, currentMode),
                if (hovered) hoverColor(type) else normalColor(type)
            )
        }
    }

    /**
     * Draws a single button with the given layout, label, and color.
     *
     * @param graphics The graphics object to render with.
     * @param layout The layout of the button.
     * @param label The label of the button.
     * @param color The color of the button.
     */
    private fun drawButton(graphics: GuiGraphicsExtractor, layout: ButtonLayout, label: String, color: Int) {
        val font = Vexo.mc.font
        graphics.fill(layout.x, layout.y, layout.x + layout.width, layout.y + layout.height, color)
        graphics.text(
            font,
            label,
            layout.x + (layout.width - font.width(label)) / 2,
            layout.y + (layout.height - font.lineHeight) / 2,
            ScreenshotColors.handleFill,
            false
        )
    }

    /**
     * Returns the normal color for a given button type.
     *
     * @param type The button type.
     * @return The normal color for the button type.
     */
    private fun normalColor(type: ScreenshotButtonType): Int = when (type) {
        ScreenshotButtonType.SAVE -> ScreenshotColors.saveSelectionNormal
        ScreenshotButtonType.SAVE_FULL -> ScreenshotColors.saveFullNormal
        ScreenshotButtonType.MODE -> ScreenshotColors.modeButtonNormal
        ScreenshotButtonType.UNDO -> ScreenshotColors.undoButtonNormal
        ScreenshotButtonType.MOVE -> ScreenshotColors.moveButtonNormal
        ScreenshotButtonType.COPY_DELETE -> ScreenshotColors.copyDeleteNormal
    }

    /**
     * Returns the hover color for a given button type.
     *
     * @param type The button type.
     * @return The hover color for the button type.
     */
    private fun hoverColor(type: ScreenshotButtonType): Int = when (type) {
        ScreenshotButtonType.SAVE -> ScreenshotColors.saveSelectionHover
        ScreenshotButtonType.SAVE_FULL -> ScreenshotColors.saveFullHover
        ScreenshotButtonType.MODE -> ScreenshotColors.modeButtonHover
        ScreenshotButtonType.UNDO -> ScreenshotColors.undoButtonHover
        ScreenshotButtonType.MOVE -> ScreenshotColors.moveButtonHover
        ScreenshotButtonType.COPY_DELETE -> ScreenshotColors.copyDeleteHover
    }
}
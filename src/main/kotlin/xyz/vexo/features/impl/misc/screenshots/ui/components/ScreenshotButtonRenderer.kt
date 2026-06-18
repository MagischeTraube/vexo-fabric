package xyz.vexo.features.impl.misc.screenshots.ui.components

import net.minecraft.client.gui.GuiGraphicsExtractor
import xyz.vexo.Vexo
import xyz.vexo.features.impl.misc.screenshots.config.ScreenshotColors
import xyz.vexo.features.impl.misc.screenshots.ScreenshotMode
import xyz.vexo.features.impl.misc.screenshots.util.SelectionBounds
import xyz.vexo.features.impl.misc.screenshots.config.ButtonConfig

object ScreenshotButtonRenderer {
    /**
     * Renders the screenshot buttons.
     *
     * @param graphics The graphics to render on.
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
        val baseY = ButtonPositionManager.getBaseY(
            top,
            bottom,
            currentMode == ScreenshotMode.DRAW
        )

        val saveButton = ScreenshotButtonLayouts.get(ScreenshotButtonType.SAVE, baseX, baseY)
        val saveFullButton = ScreenshotButtonLayouts.get(ScreenshotButtonType.SAVE_FULL, baseX, baseY)
        val modeButton = ScreenshotButtonLayouts.get(ScreenshotButtonType.MODE, baseX, baseY)
        val undoButton = ScreenshotButtonLayouts.get(ScreenshotButtonType.UNDO, baseX, baseY)
        val moveButton = ScreenshotButtonLayouts.get(ScreenshotButtonType.MOVE, baseX, baseY)

        val isSaveHovered = saveButton.contains(mouseX, mouseY)
        val isSaveFullHovered = saveFullButton.contains(mouseX, mouseY)
        val isModeHovered = modeButton.contains(mouseX, mouseY)
        val isUndoHovered = undoButton.contains(mouseX, mouseY)
        val isMoveHovered = undoButton.contains(mouseX, mouseY) || ButtonPositionManager.isDragging()

        // Save
        graphics.fill(
            saveButton.x, saveButton.y,
            saveButton.x + saveButton.width, saveButton.y + saveButton.height,
            if (isSaveHovered) ScreenshotColors.saveSelectionHover else ScreenshotColors.saveSelectionNormal
        )
        val saveLabel = "Save"
        graphics.text(
            Vexo.mc.font,
            saveLabel,
            saveButton.x + (ButtonConfig.WIDTH - Vexo.mc.font.width(saveLabel)) / 2,
            saveButton.y + (ButtonConfig.HEIGHT - Vexo.mc.font.lineHeight) / 2,
            ScreenshotColors.handleFill,
            false
        )

        // Save full
        graphics.fill(
            saveFullButton.x, saveFullButton.y,
            saveFullButton.x + saveFullButton.width, saveFullButton.y + saveFullButton.height,
            if (isSaveFullHovered) ScreenshotColors.saveFullHover else ScreenshotColors.saveFullNormal
        )
        val saveFullLabel = "Save Full"
        graphics.text(
            Vexo.mc.font,
            saveFullLabel,
            saveFullButton.x + (ButtonConfig.WIDTH - Vexo.mc.font.width(saveFullLabel)) / 2,
            saveFullButton.y + (ButtonConfig.HEIGHT - Vexo.mc.font.lineHeight) / 2,
            ScreenshotColors.handleFill,
            false
        )

        // Mode (Draw / Select)
        graphics.fill(
            modeButton.x, modeButton.y,
            modeButton.x + modeButton.width, modeButton.y + modeButton.height,
            if (isModeHovered) ScreenshotColors.modeButtonHover else ScreenshotColors.modeButtonNormal
        )
        val modeLabel = if (currentMode == ScreenshotMode.SELECT) "Draw" else "Select"
        graphics.text(
            Vexo.mc.font,
            modeLabel,
            modeButton.x + (ButtonConfig.WIDTH - Vexo.mc.font.width(modeLabel)) / 2,
            modeButton.y + (ButtonConfig.HEIGHT - Vexo.mc.font.lineHeight) / 2,
            ScreenshotColors.handleFill,
            false
        )

        // Undo
        graphics.fill(
            undoButton.x, undoButton.y,
            undoButton.x + undoButton.width, undoButton.y + undoButton.height,
            if (isUndoHovered) ScreenshotColors.undoButtonHover else ScreenshotColors.undoButtonNormal
        )
        val undoLabel = "Undo"
        graphics.text(
            Vexo.mc.font,
            undoLabel,
            undoButton.x + (ButtonConfig.WIDTH - Vexo.mc.font.width(undoLabel)) / 2,
            undoButton.y + (ButtonConfig.HEIGHT - Vexo.mc.font.lineHeight) / 2,
            ScreenshotColors.handleFill,
            false
        )

        // Move
        graphics.fill(
            moveButton.x, moveButton.y,
            moveButton.x + moveButton.width, moveButton.y + moveButton.height,
            if (isMoveHovered) ScreenshotColors.moveButtonHover else ScreenshotColors.moveButtonNormal
        )
        val moveLabel = "Move"
        graphics.text(
            Vexo.mc.font,
            moveLabel,
            moveButton.x + (ButtonConfig.WIDTH - Vexo.mc.font.width(moveLabel)) / 2,
            moveButton.y + (ButtonConfig.HEIGHT - Vexo.mc.font.lineHeight) / 2,
            ScreenshotColors.handleFill,
            false
        )
    }
}
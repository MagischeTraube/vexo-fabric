package xyz.vexo.features.impl.misc.screenshots.ui

import net.minecraft.client.gui.GuiGraphicsExtractor
import xyz.vexo.Vexo
import xyz.vexo.features.impl.misc.screenshots.ui.components.ColorPickerRenderer
import xyz.vexo.features.impl.misc.screenshots.RectDrawAction
import xyz.vexo.features.impl.misc.screenshots.config.ScreenshotColors
import xyz.vexo.features.impl.misc.screenshots.ScreenshotHud
import xyz.vexo.features.impl.misc.screenshots.ScreenshotMode
import xyz.vexo.features.impl.misc.screenshots.util.SelectionBounds
import xyz.vexo.features.impl.misc.screenshots.config.HandleConfig
import xyz.vexo.features.impl.misc.screenshots.ui.components.ScreenshotButtonRenderer

object ScreenshotHudRenderer {
    /**
     * Renders the full screenshot overlay UI.
     *
     * @param graphics render context
     * @param mouseX mouse X position
     * @param mouseY mouse Y position
     * @param selection current selection bounds
     * @param selectionIsConfirmed whether selection is locked in
     * @param currentMode active screenshot mode
     * @param drawnRectangles list of drawn rectangles (DRAW mode)
     */
    fun renderOverlay(
        graphics: GuiGraphicsExtractor,
        mouseX: Double,
        mouseY: Double,
        selection: SelectionBounds,
        selectionIsConfirmed: Boolean,
        currentMode: ScreenshotMode,
        drawnRectangles: List<RectDrawAction>
    ) {
        renderBackground(graphics)
        renderSelectionOverlay(graphics, selection)
        renderDrawnRectangles(graphics, drawnRectangles)

        if (selectionIsConfirmed && selection.hasArea) {
            renderConfirmedSelectionUI(graphics, mouseX, mouseY, selection, currentMode)
        }
    }

    /**
     * Renders the background of the screenshot overlay.
     *
     * @param graphics render context
     */
    private fun renderBackground(graphics: GuiGraphicsExtractor) {
        if (!ScreenshotHud.hasImage()) return
        val scale = (1.0 / Vexo.mc.window.guiScale).toFloat()
        val pose = graphics.pose()
        pose.pushMatrix()
        pose.scale(scale, scale)
        graphics.blit(ScreenshotHud.BACKGROUND_TEXTURE_ID, 0, 0, Vexo.mc.window.width, Vexo.mc.window.height, 0f, 1f, 0f, 1f)
        pose.popMatrix()
    }

    /**
     * Renders the selection overlay.
     *
     * @param graphics render context
     * @param selection current selection bounds
     */
    private fun renderSelectionOverlay(graphics: GuiGraphicsExtractor, selection: SelectionBounds) {
        val screenWidth  = Vexo.mc.window.guiScaledWidth
        val screenHeight = Vexo.mc.window.guiScaledHeight

        if (!selection.hasArea) {
            graphics.fill(0, 0, screenWidth, screenHeight, ScreenshotColors.outsideSelectionOverlay)
            return
        }

        val left   = selection.left.toInt()
        val right  = selection.right.toInt()
        val top    = selection.top.toInt()
        val bottom = selection.bottom.toInt()

        graphics.fill(0,     0,      screenWidth, top,         ScreenshotColors.outsideSelectionOverlay)
        graphics.fill(0,     top,    left,        bottom,      ScreenshotColors.outsideSelectionOverlay)
        graphics.fill(right, top,    screenWidth, bottom,      ScreenshotColors.outsideSelectionOverlay)
        graphics.fill(0,     bottom, screenWidth, screenHeight, ScreenshotColors.outsideSelectionOverlay)
    }

    /**
     * Renders the confirmed selection UI elements including border,
     * handles, buttons and mode-specific tools.
     *
     * @param graphics render context
     * @param mouseX mouse X position
     * @param mouseY mouse Y position
     * @param selection current selection bounds
     * @param currentMode active screenshot mode
     */
    private fun renderConfirmedSelectionUI(
        graphics: GuiGraphicsExtractor,
        mouseX: Double,
        mouseY: Double,
        selection: SelectionBounds,
        currentMode: ScreenshotMode
    ) {
        val left   = selection.left.toInt()
        val right  = selection.right.toInt()
        val top    = selection.top.toInt()
        val bottom = selection.bottom.toInt()

        renderSelectionBorder(graphics, left, right, top, bottom)
        ScreenshotButtonRenderer.render(graphics, mouseX, mouseY, selection, currentMode)

        if (currentMode == ScreenshotMode.SELECT) {
            renderCornerHandles(graphics, left, right, top, bottom)
        }

        if (currentMode == ScreenshotMode.DRAW) {
            ColorPickerRenderer.render(graphics, selection, currentMode)
        }

        renderHintText(graphics, currentMode)
    }

    /**
     * Renders the border around the current selection area.
     *
     * @param graphics render context
     * @param left left boundary of selection
     * @param right right boundary of selection
     * @param top top boundary of selection
     * @param bottom bottom boundary of selection
     */
    private fun renderSelectionBorder(graphics: GuiGraphicsExtractor, left: Int, right: Int, top: Int, bottom: Int) {
        graphics.fill(left,     top,        right,      top + 1,    ScreenshotColors.handleFill)
        graphics.fill(left,     bottom - 1, right,      bottom,     ScreenshotColors.handleFill)
        graphics.fill(left,     top,        left + 1,   bottom,     ScreenshotColors.handleFill)
        graphics.fill(right - 1, top,       right,      bottom,     ScreenshotColors.handleFill)
    }

    /**
     * Renders corner resize handles for the selection box.
     *
     * @param graphics render context
     * @param left left boundary of selection
     * @param right right boundary of selection
     * @param top top boundary of selection
     * @param bottom bottom boundary of selection
     */
    private fun renderCornerHandles(graphics: GuiGraphicsExtractor, left: Int, right: Int, top: Int, bottom: Int) {
        drawHandle(graphics, left,                          top)
        drawHandle(graphics, right - HandleConfig.SIZE,     top)
        drawHandle(graphics, left,                          bottom - HandleConfig.SIZE)
        drawHandle(graphics, right - HandleConfig.SIZE,     bottom - HandleConfig.SIZE)
    }

    /**
     * Draws a handle.
     *
     * @param graphics render context
     * @param posX x-coordinate of the handle
     * @param posY y-coordinate of the handle
     */
    private fun drawHandle(graphics: GuiGraphicsExtractor, posX: Int, posY: Int) {
        val size = HandleConfig.SIZE
        graphics.fill(posX - 1,      posY - 1,      posX + size + 1, posY + size + 1, ScreenshotColors.handleBorder)
        graphics.fill(posX,          posY,           posX + size,     posY + size,     ScreenshotColors.handleFill)
    }

    /**
     * Renders the drawn rectangles.
     *
     * @param graphics render context
     * @param drawnRectangles list of drawn rectangles
     */
    private fun renderDrawnRectangles(graphics: GuiGraphicsExtractor, drawnRectangles: List<RectDrawAction>) {
        for (rect in drawnRectangles) {
            val l = rect.bounds.left.toInt()
            val r = rect.bounds.right.toInt()
            val t = rect.bounds.top.toInt()
            val b = rect.bounds.bottom.toInt()
            graphics.fill(l, t, r, t + 2, rect.color.rgb)
            graphics.fill(l, b - 2, r, b, rect.color.rgb)
            graphics.fill(l, t, l + 2, b, rect.color.rgb)
            graphics.fill(r - 2, t, r, b, rect.color.rgb)
        }
    }

    /**
     * Renders the hint text.
     *
     * @param graphics render context
     * @param currentMode active screenshot mode
     */
    private fun renderHintText(graphics: GuiGraphicsExtractor, currentMode: ScreenshotMode) {
        val hintText = if (currentMode == ScreenshotMode.SELECT)
            "Enter = Save   Esc = Exit   Click = New Selection   R = Reset"
        else
            "Draw Mode: Drag to draw. Click 'Undo' to remove last box. R = Reset"

        graphics.text(
            Vexo.mc.font, hintText,
            (Vexo.mc.window.guiScaledWidth - Vexo.mc.font.width(hintText)) / 2,
            10,
            ScreenshotColors.hintText, false
        )
    }
}
package xyz.vexo.features.impl.misc.screenshots

import net.minecraft.client.gui.screens.Screen
import org.lwjgl.glfw.GLFW
import xyz.vexo.Vexo.mc
import xyz.vexo.features.impl.misc.ScreenshotActions
import xyz.vexo.features.impl.misc.screenshots.config.ButtonConfig
import xyz.vexo.features.impl.misc.screenshots.config.ColorPickerConfig
import xyz.vexo.features.impl.misc.screenshots.config.HandleConfig
import xyz.vexo.features.impl.misc.screenshots.config.ScreenshotColors
import xyz.vexo.features.impl.misc.screenshots.ui.components.ButtonPositionManager
import xyz.vexo.features.impl.misc.screenshots.ui.components.ScreenshotButtonLayouts
import xyz.vexo.features.impl.misc.screenshots.ui.components.ScreenshotButtonType
import xyz.vexo.features.impl.misc.screenshots.util.SelectionBounds
import kotlin.math.max
import kotlin.math.min

object ScreenshotInputHandler {
    /**
     * Handles mouse click events for the screenshot feature.
     *
     * @param mouseX The x-coordinate of the mouse click.
     * @param mouseY The y-coordinate of the mouse click.
     * @param screen The current screen.
     */
    fun handleMouseClick(mouseX: Double, mouseY: Double, screen: Screen) {
        if (ScreenshotHud.selectionIsConfirmed) {
            val left   = ScreenshotHud.selection.left.toInt()
            val right  = ScreenshotHud.selection.right.toInt()
            val top    = ScreenshotHud.selection.top.toInt()
            val bottom = ScreenshotHud.selection.bottom.toInt()

            val baseX = ButtonPositionManager.getBaseX(left, right)
            val baseY = ButtonPositionManager.getBaseY(top, bottom, ScreenshotHud.currentMode == ScreenshotMode.DRAW)

            val saveButton = ScreenshotButtonLayouts.get(ScreenshotButtonType.SAVE, baseX, baseY)
            val saveFullButton = ScreenshotButtonLayouts.get(ScreenshotButtonType.SAVE_FULL, baseX, baseY)
            val modeButton = ScreenshotButtonLayouts.get(ScreenshotButtonType.MODE, baseX, baseY)
            val undoButton = ScreenshotButtonLayouts.get(ScreenshotButtonType.UNDO, baseX, baseY)
            val moveButton = ScreenshotButtonLayouts.get(ScreenshotButtonType.MOVE, baseX, baseY)

            when {
                saveButton.contains(mouseX, mouseY)     -> { ScreenshotHud.saveScreenshot(screen, cropToSelection = true);  return }
                saveFullButton.contains(mouseX, mouseY) -> { ScreenshotHud.saveScreenshot(screen, cropToSelection = false); return }
                modeButton.contains(mouseX, mouseY)     -> {
                    ScreenshotHud.currentMode = if (ScreenshotHud.currentMode == ScreenshotMode.SELECT) ScreenshotMode.DRAW else ScreenshotMode.SELECT
                    return
                }
                undoButton.contains(mouseX, mouseY)     -> {
                    ScreenshotHud.undoLastRectangle()
                    return
                }
                moveButton.contains(mouseX, mouseY)     -> {
                    ButtonPositionManager.startDragging(mouseX, mouseY, baseX, baseY)
                    return
                }
            }

            if (ScreenshotHud.currentMode == ScreenshotMode.DRAW) {
                val size = ColorPickerConfig.SIZE
                val gap = ColorPickerConfig.GAP
                val buttonsBaseX = ButtonPositionManager.getBaseX(left, right)
                val startX = buttonsBaseX + (ButtonConfig.TOTAL_WIDTH / 2) - (ColorPickerConfig.TOTAL_WIDTH / 2)
                val y = ButtonPositionManager.getBaseY(top, bottom, true) + ButtonConfig.HEIGHT + ColorPickerConfig.Y_OFFSET_BELOW_BUTTONS

                for (i in ScreenshotColors.drawColors.indices) {
                    val x = startX + (i * (size + gap))
                    if (mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size) {
                        ScreenshotHud.selectedDrawColor = ScreenshotColors.drawColors[i]
                        return
                    }
                }

                if (mouseX >= ScreenshotHud.selection.left && mouseX <= ScreenshotHud.selection.right && mouseY >= ScreenshotHud.selection.top && mouseY <= ScreenshotHud.selection.bottom) {
                    ScreenshotHud.activeRectStart = Pair(mouseX, mouseY)
                    ScreenshotHud.drawnRectangles.add(RectDrawAction(SelectionBounds(mouseX, mouseX, mouseY, mouseY), ScreenshotHud.selectedDrawColor))
                }
                return
            }

            val clickedHandle = findHandleAt(mouseX, mouseY)
            if (clickedHandle != null) {
                ScreenshotHud.activeDrag = HandleDragState(clickedHandle, mouseX, mouseY, ScreenshotHud.selection)
                return
            }

            ScreenshotHud.selectionIsConfirmed = false
            ScreenshotHud.activeDrag = null
            ScreenshotHud.drawnRectangles.clear()
        }

        ScreenshotHud.selection = SelectionBounds(left = mouseX, right = mouseX, top = mouseY, bottom = mouseY)
    }

    /**
     * Handles mouse drag events for the screenshot feature.
     *
     * @param mouseX The x-coordinate of the mouse drag.
     * @param mouseY The y-coordinate of the mouse drag.
     */
    fun handleMouseDrag(mouseX: Double, mouseY: Double) {
        val screenWidth  = mc.window.guiScaledWidth.toDouble()
        val screenHeight = mc.window.guiScaledHeight.toDouble()

        if (ButtonPositionManager.updateDragging(mouseX, mouseY)) {
            return
        }

        if (ScreenshotHud.selectionIsConfirmed && ScreenshotHud.currentMode == ScreenshotMode.DRAW) {
            val start = ScreenshotHud.activeRectStart ?: return
            if (ScreenshotHud.drawnRectangles.isNotEmpty()) {
                val clampedX = mouseX.coerceIn(ScreenshotHud.selection.left, ScreenshotHud.selection.right)
                val clampedY = mouseY.coerceIn(ScreenshotHud.selection.top, ScreenshotHud.selection.bottom)

                val updatedBounds = SelectionBounds(
                    left = min(start.first, clampedX),
                    right = max(start.first, clampedX),
                    top = min(start.second, clampedY),
                    bottom = max(start.second, clampedY)
                )

                ScreenshotHud.drawnRectangles[ScreenshotHud.drawnRectangles.lastIndex] = RectDrawAction(updatedBounds, ScreenshotHud.selectedDrawColor)
            }
            return
        }

        val drag = ScreenshotHud.activeDrag
        if (ScreenshotHud.selectionIsConfirmed && drag != null) {
            val deltaX = mouseX - drag.originX
            val deltaY = mouseY - drag.originY
            val snapped = drag.selectionAtDragStart

            ScreenshotHud.selection = when (drag.handle) {
                CornerHandle.TOP_LEFT     -> ScreenshotHud.selection.copy(left  = snapped.left  + deltaX, top    = snapped.top    + deltaY)
                CornerHandle.TOP_RIGHT    -> ScreenshotHud.selection.copy(right = snapped.right + deltaX, top    = snapped.top    + deltaY)
                CornerHandle.BOTTOM_LEFT  -> ScreenshotHud.selection.copy(left  = snapped.left  + deltaX, bottom = snapped.bottom + deltaY)
                CornerHandle.BOTTOM_RIGHT -> ScreenshotHud.selection.copy(right = snapped.right + deltaX, bottom = snapped.bottom + deltaY)
            }
            ScreenshotHud.selection = ScreenshotHud.selection.clampedTo(screenWidth, screenHeight).normalized()
            return
        }

        if (!ScreenshotHud.selectionIsConfirmed) {
            val origin = SelectionBounds(
                left = min(ScreenshotHud.selection.left, mouseX).coerceIn(0.0, screenWidth),
                right = max(ScreenshotHud.selection.left, mouseX).coerceIn(0.0, screenWidth),
                top = min(ScreenshotHud.selection.top, mouseY).coerceIn(0.0, screenHeight),
                bottom = max(ScreenshotHud.selection.top, mouseY).coerceIn(0.0, screenHeight)
            )
            ScreenshotHud.selection = origin
        }
    }

    /**
     * Handles mouse release events for the screenshot feature.
     *
     * @param screen The current screen.
     */
    fun handleMouseRelease(screen: Screen) {
        if (ButtonPositionManager.isDragging()) {
            ButtonPositionManager.stopDragging()
            return
        }

        if (ScreenshotHud.selectionIsConfirmed) {
            if (ScreenshotHud.currentMode == ScreenshotMode.DRAW) {
                ScreenshotHud.activeRectStart = null
                if (ScreenshotHud.drawnRectangles.isNotEmpty() && !ScreenshotHud.drawnRectangles.last().bounds.isLargeEnough) {
                    ScreenshotHud.drawnRectangles.removeAt(ScreenshotHud.drawnRectangles.lastIndex)
                }
            }
            ScreenshotHud.activeDrag = null
            return
        }

        if (ScreenshotHud.selection.isLargeEnough) {
            if (!ScreenshotActions.editableSelection) {
                ScreenshotHud.saveScreenshot(screen, cropToSelection = true)
            } else {
                ScreenshotHud.selectionIsConfirmed = true
            }
        }
        ScreenshotHud.activeDrag = null
    }

    /**
     * Handles key press events for the screenshot feature.
     *
     * @param key The key code of the pressed key.
     * @param screen The current screen.
     * @return True if the key press was handled, false otherwise.
     */
    fun handleKeyPress(key: Int, screen: Screen): Boolean {
        return when (key) {
            GLFW.GLFW_KEY_ESCAPE -> {
                ScreenshotActions.displayScreenshotHud = false
                ScreenshotHud.destroy()
                true
            }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (ScreenshotHud.selectionIsConfirmed) ScreenshotHud.saveScreenshot(screen, cropToSelection = true)
                false
            }
            GLFW.GLFW_KEY_R -> {
                ScreenshotHud.reset()
                true
            }
            else -> false
        }
    }

    /**
     * Finds the handle (corner) that the mouse is currently over.
     *
     * @param mouseX The x-coordinate of the mouse.
     * @param mouseY The y-coordinate of the mouse.
     * @return The corner handle that the mouse is over, or null if none.
     */
    private fun findHandleAt(mouseX: Double, mouseY: Double): CornerHandle? {
        val hitbox = HandleConfig.HITBOX_RADIUS
        val size = HandleConfig.SIZE.toDouble()
        val selection = ScreenshotHud.selection

        val handles = listOf(
            CornerHandle.TOP_LEFT to Pair(selection.left + size / 2, selection.top + size / 2),
            CornerHandle.TOP_RIGHT to Pair(selection.right - size / 2, selection.top + size / 2),
            CornerHandle.BOTTOM_LEFT to Pair(selection.left + size / 2, selection.bottom - size / 2),
            CornerHandle.BOTTOM_RIGHT to Pair(selection.right - size / 2, selection.bottom - size / 2)
        )

        return handles
            .mapNotNull { (handle, pos) ->
                val (x, y) = pos
                val inRange = mouseX >= x - size / 2 - hitbox && mouseX <= x + size / 2 + hitbox && mouseY >= y - size / 2 - hitbox && mouseY <= y + size / 2 + hitbox
                if (!inRange) return@mapNotNull null

                val dx = mouseX - x
                val dy = mouseY - y
                handle to (dx * dx + dy * dy)
            }
            .minByOrNull { it.second }
            ?.first
    }
}

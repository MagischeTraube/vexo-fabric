package xyz.vexo.features.impl.misc.screenshots.ui.components

import xyz.vexo.Vexo
import xyz.vexo.features.impl.misc.screenshots.config.ButtonConfig
import xyz.vexo.features.impl.misc.screenshots.config.ColorPickerConfig

/**
 * Manages the position of the screenshot buttons.
 */
object ButtonPositionManager {

    private var customX: Int? = null
    private var customY: Int? = null

    private var dragging = false

    private var dragStartMouseX = 0.0
    private var dragStartMouseY = 0.0

    private var dragStartX = 0
    private var dragStartY = 0

    /**
     * Returns the base X position of the buttons.
     *
     * @param left The leftmost edge of the selection.
     * @param right The rightmost edge of the selection.
     * @return The base X position of the buttons.
     */
    fun getBaseX(left: Int, right: Int): Int {
        return customX ?: (((left + right) / 2) - ButtonConfig.TOTAL_WIDTH / 2)
    }

    /**
     * Returns the base Y position of the buttons.
     *
     * @param selectionTop The topmost edge of the selection.
     * @param selectionBottom The bottommost edge of the selection.
     * @param drawMode Whether the color picker is enabled.
     * @return The base Y position of the buttons.
     */
    fun getBaseY(
        selectionTop: Int,
        selectionBottom: Int,
        drawMode: Boolean
    ): Int {
        customY?.let { return it }

        val screenHeight = Vexo.mc.window.guiScaledHeight

        var requiredHeight =
            ButtonConfig.Y_OFFSET_BELOW_SELECTION + ButtonConfig.HEIGHT

        if (drawMode) {
            requiredHeight +=
                ColorPickerConfig.Y_OFFSET_BELOW_BUTTONS +
                        ColorPickerConfig.SIZE
        }

        return if (selectionBottom + requiredHeight > screenHeight) {
            (selectionBottom - requiredHeight)
                .coerceAtLeast(selectionTop)
        } else {
            selectionBottom + ButtonConfig.Y_OFFSET_BELOW_SELECTION
        }
    }

    /**
     * Starts dragging the buttons.
     *
     * @param mouseX The current mouse X position.
     * @param mouseY The current mouse Y position.
     * @param currentX The current X position of the buttons.
     * @param currentY The current Y position of the buttons.
     */
    fun startDragging(
        mouseX: Double,
        mouseY: Double,
        currentX: Int,
        currentY: Int
    ) {
        dragging = true

        dragStartMouseX = mouseX
        dragStartMouseY = mouseY

        dragStartX = currentX
        dragStartY = currentY
    }

    /**
     * Updates the position of the buttons while dragging.
     *
     * @param mouseX The current mouse X position.
     * @param mouseY The current mouse Y position.
     * @return True if the buttons were moved, false otherwise.
     */
    fun updateDragging(mouseX: Double, mouseY: Double): Boolean {
        if (!dragging) return false

        val deltaX = mouseX - dragStartMouseX
        val deltaY = mouseY - dragStartMouseY

        customX = (dragStartX + deltaX).toInt()
        customY = (dragStartY + deltaY).toInt()

        return true
    }

    /**
     * Stops dragging the buttons.
     */
    fun stopDragging() {
        dragging = false
    }

    /**
     * Returns whether the buttons are currently being dragged.
     *
     * @return True if the buttons are being dragged, false otherwise.
     */
    fun isDragging(): Boolean = dragging

    /**
     * Resets the button position manager.
     */
    fun reset() {
        customX = null
        customY = null
        dragging = false
    }
}
package xyz.vexo.features.impl.misc.screenshots.ui.components

import xyz.vexo.features.impl.misc.screenshots.config.ButtonConfig

/**
 * Represents the layout of a button.
 *
 * @property x The x-coordinate of the button.
 * @property y The y-coordinate of the button.
 * @property width The width of the button.
 * @property height The height of the button.
 */
data class ButtonLayout(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {
    fun contains(mouseX: Double, mouseY: Double) =
        mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
}

/**
 * Enum representing the different types of screenshot buttons.
 *
 * @property index The index of the button.
 */
enum class ScreenshotButtonType(val index: Int) {
    SAVE(0),
    SAVE_FULL(1),
    MODE(2),
    UNDO(3),
    MOVE(4)
}

/**
 * Manages the layout of the screenshot buttons.
 */
object ScreenshotButtonLayouts {

    fun get(type: ScreenshotButtonType, baseX: Int, baseY: Int): ButtonLayout {
        val x = baseX + type.index * (ButtonConfig.WIDTH + ButtonConfig.GAP)

        return ButtonLayout(
            x,
            baseY,
            ButtonConfig.WIDTH,
            ButtonConfig.HEIGHT
        )
    }
}
package xyz.vexo.features.impl.misc.screenshots.ui.components

import kotlin.math.max
import xyz.vexo.features.impl.misc.screenshots.ScreenshotMode
import xyz.vexo.features.impl.misc.screenshots.config.ButtonConfig
import xyz.vexo.Vexo

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
    COPY_DELETE(0),
    SAVE(1),
    SAVE_FULL(2),
    MODE(3),
    UNDO(4),
    MOVE(5),
}

/**
 * Manages the layout of the screenshot buttons.
 */
object ScreenshotButtonLayouts {

    private const val PADDING_X = 8

    fun label(type: ScreenshotButtonType, currentMode: ScreenshotMode): String = when (type) {
        ScreenshotButtonType.COPY_DELETE -> "Copy and Delete"
        ScreenshotButtonType.SAVE -> "Save"
        ScreenshotButtonType.SAVE_FULL -> "Save Full"
        ScreenshotButtonType.MODE -> if (currentMode == ScreenshotMode.SELECT) "Mode: Draw" else "Mode: Select"
        ScreenshotButtonType.UNDO -> "Undo"
        ScreenshotButtonType.MOVE -> "Move Buttons"
    }

    private fun widthOf(type: ScreenshotButtonType): Int {
        val font = Vexo.mc.font
        val textWidth = if (type == ScreenshotButtonType.MODE) {
            max(font.width("Mode: Draw"), font.width("Mode: Select"))
        } else {
            font.width(label(type, ScreenshotMode.SELECT))
        }
        return max(ButtonConfig.WIDTH, textWidth + PADDING_X * 2)
    }

    fun totalWidth(): Int {
        val types = ScreenshotButtonType.entries
        return types.sumOf { widthOf(it) } + ButtonConfig.GAP * (types.size - 1)
    }

    fun get(type: ScreenshotButtonType, baseX: Int, baseY: Int): ButtonLayout {
        val offset = ScreenshotButtonType.entries
            .filter { it.index < type.index }
            .sumOf { widthOf(it) + ButtonConfig.GAP }

        return ButtonLayout(baseX + offset, baseY, widthOf(type), ButtonConfig.HEIGHT)
    }
}
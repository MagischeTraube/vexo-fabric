package xyz.vexo.clickgui.components

import java.awt.Color
import gg.essential.elementa.components.GradientComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.dsl.*
import xyz.vexo.clickgui.gpx

class AlphaBar(
    initialBaseColor: Color,
    initialAlpha: Int,
    private val onAlphaChange: (Int) -> Unit
) : UIContainer() {

    companion object {
        const val HEIGHT = 14f
        const val POINT_SIZE = 6f
    }

    private var handle: UIRoundedRectangle? = null
    private var gradient: GradientComponent? = null
    private var currentAlpha = initialAlpha
    private var baseColor: Color

    init {
        baseColor = initialBaseColor
        constrain {
            width = 100.percent()
            height = HEIGHT.gpx()
        }

        UIContainer().constrain {
            width = 100.percent()
            height = 100.percent()
        }.also { it.setColor(ClickGuiColor.DARK_BACKGROUND_COLOR) } childOf this

        gradient = GradientComponent(
            Color(baseColor.red, baseColor.green, baseColor.blue, 0),
            Color(baseColor.red, baseColor.green, baseColor.blue, 255),
            GradientComponent.GradientDirection.LEFT_TO_RIGHT
        ).constrain {
            width = 100.percent()
            height = 100.percent()
        } childOf this

        handle = UIRoundedRectangle(POINT_SIZE / 2f).constrain {
            x = (currentAlpha / 255f * 100).percent() - (POINT_SIZE / 2f).gpx()
            y = 0.gpx()
            width = POINT_SIZE.gpx()
            height = 100.percent()
        }.also { it.setColor(ClickGuiColor.WHITE_SLIDER_HANDLE_COLOR) } childOf this

        var dragging = false
        fun updateFromMouse(mouseX: Float) {
            val width = getWidth()
            val percentage = mouseX.coerceIn(0f, width) / width
            currentAlpha = (percentage * 255).toInt().coerceIn(0, 255)
            handle!!.setX((percentage * 100).percent() - (POINT_SIZE / 2f).gpx())
            onAlphaChange(currentAlpha)
        }

        onMouseClick { dragging = true }
        onMouseRelease { dragging = false }
        onMouseDrag { mouseX, _, _ ->
            if (!dragging) return@onMouseDrag
            updateFromMouse(mouseX)
        }
    }

    fun setAlpha(alpha: Int) {
        currentAlpha = alpha
        handle!!.setX((alpha / 255f * 100).percent() - (POINT_SIZE / 2f).gpx())
    }

    fun updateColor(color: Color) {
        baseColor = color
        gradient!!.setStartColor(Color(color.red, color.green, color.blue, 0))
        gradient!!.setEndColor(Color(color.red, color.green, color.blue, 255))
    }
}

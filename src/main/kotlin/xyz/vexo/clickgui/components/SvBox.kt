package xyz.vexo.clickgui.components

import java.awt.Color
import gg.essential.elementa.components.GradientComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.dsl.*
import xyz.vexo.clickgui.gpx

class SvBox(
    initialColor: Color,
    private val onChange: (saturation: Float, brightness: Float) -> Unit,
    initHeight: Float = 100f
) : UIContainer() {

    companion object {
        const val HEIGHT = 100f
        const val POINT_SIZE = 12f
    }

    private var pointer: UIRoundedRectangle? = null
    private var hueGradient: GradientComponent? = null
    private var currentHue = 0f
    private var currentSaturation = 0f
    private var currentBrightness = 1f
    private val boxHeight: Float

    init {
        val hsb = Color.RGBtoHSB(initialColor.red, initialColor.green, initialColor.blue, null)
        currentHue = hsb[0]
        currentSaturation = hsb[1]
        currentBrightness = hsb[2]
        boxHeight = initHeight

        constrain {
            width = 100.percent()
            height = initHeight.gpx()
        }

        hueGradient = GradientComponent(
            Color.WHITE,
            Color.WHITE,
            GradientComponent.GradientDirection.LEFT_TO_RIGHT
        ).constrain {
            width = 100.percent()
            height = 100.percent()
        } childOf this
        hueGradient!!.setEndColor(Color.getHSBColor(currentHue, 1f, 1f))

        GradientComponent(
            Color(0, 0, 0, 0),
            Color(0, 0, 0, 255),
            GradientComponent.GradientDirection.TOP_TO_BOTTOM
        ).constrain {
            width = 100.percent()
            height = 100.percent()
        } childOf this

        pointer = UIRoundedRectangle(POINT_SIZE / 2f).constrain {
            x = (currentSaturation * 100).percent() - (POINT_SIZE / 2f).gpx()
            y = ((1 - currentBrightness) * boxHeight).gpx() - (POINT_SIZE / 2f).gpx()
            width = POINT_SIZE.gpx()
            height = POINT_SIZE.gpx()
        }.also { it.setColor(Color.WHITE) } childOf this

        var dragging = false
        fun updateFromMouse(mouseX: Float, mouseY: Float) {
            val width = getWidth()
            currentSaturation = mouseX.coerceIn(0f, width) / width
            currentBrightness = 1f - mouseY.coerceIn(0f, boxHeight) / boxHeight
            pointer!!.setX((currentSaturation * 100).percent() - (POINT_SIZE / 2f).gpx())
            pointer!!.setY(((1 - currentBrightness) * boxHeight).gpx() - (POINT_SIZE / 2f).gpx())
            onChange(currentSaturation, currentBrightness)
        }

        onMouseClick { dragging = true }
        onMouseRelease { dragging = false }
        onMouseDrag { mouseX, mouseY, _ ->
            if (!dragging) return@onMouseDrag
            updateFromMouse(mouseX, mouseY)
        }
    }

    fun updateColor(color: Color) {
        val hsb = Color.RGBtoHSB(color.red, color.green, color.blue, null)
        if (hsb[1] > 0.01f) {
            currentHue = hsb[0]
            hueGradient!!.setEndColor(Color.getHSBColor(currentHue, 1f, 1f))
        }
    }
}

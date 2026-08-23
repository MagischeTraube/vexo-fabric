package xyz.vexo.clickgui.components

import java.awt.Color
import gg.essential.elementa.components.GradientComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.dsl.*
import xyz.vexo.clickgui.gpx
import xyz.vexo.clickgui.theme.Theme

class HueBar(
    initialHue: Float,
    private val onHueChange: (Float) -> Unit
) : UIContainer() {

    companion object {
        const val HEIGHT = 14f
        const val POINT_SIZE = 6f
        const val SEGMENT_OVERLAP = 0.6f
    }

    private var handle: UIRoundedRectangle? = null
    private var currentHue = initialHue

    init {
        constrain {
            width = 100.percent()
            height = HEIGHT.gpx()
        }

        val hueColors = (0..6).map { Color.getHSBColor(it / 6f, 1f, 1f) }
        val segmentPercent = 100f / 6f
        for (i in 0 until 6) {
            GradientComponent(
                hueColors[i],
                hueColors[i + 1],
                GradientComponent.GradientDirection.LEFT_TO_RIGHT
            ).constrain {
                x = (i * segmentPercent).percent() - (if (i == 0) 0f else SEGMENT_OVERLAP).gpx()
                width = segmentPercent.percent() +
                        (if (i == 0) SEGMENT_OVERLAP else SEGMENT_OVERLAP * 2f).gpx()
                height = 100.percent()
            } childOf this
        }

        handle = UIRoundedRectangle(POINT_SIZE / 2f).constrain {
            x = (currentHue * 100).percent() - (POINT_SIZE / 2f).gpx()
            y = 0.gpx()
            width = POINT_SIZE.gpx()
            height = 100.percent()
        }.also { it.setColor(Theme.handle()) } childOf this

        var dragging = false
        fun updateFromMouse(mouseX: Float) {
            val width = getWidth()
            currentHue = mouseX.coerceIn(0f, width) / width
            handle!!.setX((currentHue * 100).percent() - (POINT_SIZE / 2f).gpx())
            onHueChange(currentHue)
        }

        onMouseClick { dragging = true }
        onMouseRelease { dragging = false }
        onMouseDrag { mouseX, _, _ ->
            if (!dragging) return@onMouseDrag
            updateFromMouse(mouseX)
        }
    }
}

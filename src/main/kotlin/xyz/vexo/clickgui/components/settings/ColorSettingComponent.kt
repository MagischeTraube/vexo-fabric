package xyz.vexo.clickgui.components.settings

import java.awt.Color
import gg.essential.elementa.components.GradientComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.UIText
import gg.essential.elementa.dsl.*
import xyz.vexo.clickgui.gpx
import xyz.vexo.clickgui.theme.Theme
import xyz.vexo.config.impl.ColorSetting
import xyz.vexo.clickgui.components.ClickGuiColor

/**
 * Color setting component - expandable picker with hue bar, saturation/brightness box, and alpha slider.
 */
class ColorSettingComponent(
    private val setting: ColorSetting
) : UIContainer() {

    private var expanded = false
    private val sliderContainer: UIContainer
    private val colorPreview: UIRoundedRectangle

    private var hue: Float
    private var saturation: Float
    private var brightness: Float
    private var alpha: Int = setting.getCurrentValue().alpha

    private lateinit var svBoxGradient: GradientComponent
    private lateinit var svPointer: UIRoundedRectangle
    private lateinit var hueHandle: UIRoundedRectangle
    private lateinit var alphaGradient: GradientComponent
    private lateinit var alphaHandle: UIRoundedRectangle

    companion object {
        private const val HUE_BAR_HEIGHT = 14f
        private const val SV_BOX_HEIGHT = 100f
        private const val ALPHA_BAR_HEIGHT = 14f
        private const val SPACING = 10f
        private const val POINT_SIZE = 12f
        private const val SEGMENT_OVERLAP = 0.6f
    }

    init {
        val initial = setting.getCurrentValue()
        val hsb = Color.RGBtoHSB(initial.red, initial.green, initial.blue, null)
        hue = hsb[0]; saturation = hsb[1]; brightness = hsb[2]

        constrain {
            width = 100.percent()
            height = 30.gpx()
        }

        UIText(setting.name).constrain {
            x = 14.gpx()
            y = 8.gpx()
            textScale = 1.gpx()
        }.setColor(Theme.textPrimary()) childOf this

        colorPreview = UIRoundedRectangle(4f).constrain {
            x = 14.gpx(true)
            y = 6.gpx()
            width = 30.gpx()
            height = 20.gpx()
        }.apply { setColor(initial) } childOf this

        colorPreview.onMouseClick { toggleExpanded() }

        val contentHeight = HUE_BAR_HEIGHT + SPACING + SV_BOX_HEIGHT +
                if (setting.allowAlpha) SPACING + ALPHA_BAR_HEIGHT else 0f

        sliderContainer = UIContainer().constrain {
            x = 14.gpx()
            y = 32.gpx()
            width = 100.percent() - 28.gpx()
            height = contentHeight.gpx()
        } childOf this

        sliderContainer.hide(instantly = true)

        buildHueBar() childOf sliderContainer
        buildSvBox().constrain { y = (HUE_BAR_HEIGHT + SPACING).gpx() } childOf sliderContainer

        if (setting.allowAlpha) {
            buildAlphaBar().constrain {
                y = (HUE_BAR_HEIGHT + SPACING + SV_BOX_HEIGHT + SPACING).gpx()
            } childOf sliderContainer
        }
    }

    private fun toggleExpanded() {
        expanded = !expanded
        val contentHeight = HUE_BAR_HEIGHT + SPACING + SV_BOX_HEIGHT +
                if (setting.allowAlpha) SPACING + ALPHA_BAR_HEIGHT else 0f
        if (expanded) {
            sliderContainer.unhide()
            constrain { height = (32f + contentHeight + 10f).gpx() }
        } else {
            sliderContainer.hide()
            constrain { height = 30.gpx() }
        }
    }

    private fun applyColor() {
        val rgb = Color.getHSBColor(hue, saturation, brightness)
        val c = Color(rgb.red, rgb.green, rgb.blue, alpha)
        setting.updateValue(c)
        colorPreview.setColor(c)
        if (::alphaGradient.isInitialized) {
            alphaGradient.setStartColor(Color(rgb.red, rgb.green, rgb.blue, 0))
            alphaGradient.setEndColor(Color(rgb.red, rgb.green, rgb.blue, 255))
        }
    }

    private fun buildHueBar(): UIContainer {
        val bar = UIContainer().constrain {
            width = 100.percent()
            height = HUE_BAR_HEIGHT.gpx()
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
            } childOf bar
        }

        hueHandle = UIRoundedRectangle(3f).constrain {
            x = (hue * 100).percent() - 3.gpx()
            y = 0.gpx()
            width = 6.gpx()
            height = HUE_BAR_HEIGHT.gpx()
        }.also { it.setColor(Theme.handle()) } childOf bar

        var dragging = false
        fun updateFromMouse(mouseX: Float) {
            val width = bar.getWidth()
            hue = mouseX.coerceIn(0f, width) / width
            hueHandle.setX((hue * 100).percent() - 3.gpx())
            svBoxGradient.setEndColor(Color.getHSBColor(hue, 1f, 1f))
            applyColor()
        }

        bar.onMouseClick { dragging = true }
        bar.onMouseRelease { dragging = false }
        bar.onMouseDrag { mouseX, _, _ ->
            if (!dragging) return@onMouseDrag
            updateFromMouse(mouseX)
        }

        return bar
    }

    private fun buildSvBox(): UIContainer {
        val svBox = UIContainer().constrain {
            width = 100.percent()
            height = SV_BOX_HEIGHT.gpx()
        }

        svBoxGradient = GradientComponent(
            Color.WHITE,
            Color.getHSBColor(hue, 1f, 1f),
            GradientComponent.GradientDirection.LEFT_TO_RIGHT
        ).constrain {
            width = 100.percent()
            height = 100.percent()
        } childOf svBox

        GradientComponent(
            Color(0, 0, 0, 0),
            Color(0, 0, 0, 255),
            GradientComponent.GradientDirection.TOP_TO_BOTTOM
        ).constrain {
            width = 100.percent()
            height = 100.percent()
        } childOf svBox

        svPointer = UIRoundedRectangle(POINT_SIZE / 2f).constrain {
            x = (saturation * 100).percent() - (POINT_SIZE / 2f).gpx()
            y = ((1 - brightness) * SV_BOX_HEIGHT).gpx() - (POINT_SIZE / 2f).gpx()
            width = POINT_SIZE.gpx()
            height = POINT_SIZE.gpx()
        }.also { it.setColor(Color.WHITE) } childOf svBox

        var dragging = false
        fun updateFromMouse(mouseX: Float, mouseY: Float) {
            val width = svBox.getWidth()
            val height = svBox.getHeight()
            saturation = mouseX.coerceIn(0f, width) / width
            brightness = 1f - mouseY.coerceIn(0f, height) / height
            svPointer.setX((saturation * 100).percent() - (POINT_SIZE / 2f).gpx())
            svPointer.setY(((1 - brightness) * SV_BOX_HEIGHT).gpx() - (POINT_SIZE / 2f).gpx())
            applyColor()
        }

        svBox.onMouseClick { dragging = true }
        svBox.onMouseRelease { dragging = false }
        svBox.onMouseDrag { mouseX, mouseY, _ ->
            if (!dragging) return@onMouseDrag
            updateFromMouse(mouseX, mouseY)
        }

        return svBox
    }

    private fun buildAlphaBar(): UIContainer {
        val bar = UIContainer().constrain {
            width = 100.percent()
            height = ALPHA_BAR_HEIGHT.gpx()
        }

        UIContainer().constrain {
            width = 100.percent()
            height = 100.percent()
        }.also { it.setColor(ClickGuiColor.DARK_BACKGROUND_COLOR) } childOf bar

        val rgb = Color.getHSBColor(hue, saturation, brightness)
        alphaGradient = GradientComponent(
            Color(rgb.red, rgb.green, rgb.blue, 0),
            Color(rgb.red, rgb.green, rgb.blue, 255),
            GradientComponent.GradientDirection.LEFT_TO_RIGHT
        ).constrain {
            width = 100.percent()
            height = 100.percent()
        } childOf bar

        alphaHandle = UIRoundedRectangle(3f).constrain {
            x = (alpha / 255f * 100).percent() - 3.gpx()
            y = 0.gpx()
            width = 6.gpx()
            height = ALPHA_BAR_HEIGHT.gpx()
        }.also { it.setColor(ClickGuiColor.WHITE_SLIDER_HANDLE_COLOR) } childOf bar

        var dragging = false
        fun updateFromMouse(mouseX: Float) {
            val width = bar.getWidth()
            val percentage = mouseX.coerceIn(0f, width) / width
            alpha = (percentage * 255).toInt().coerceIn(0, 255)
            alphaHandle.setX((percentage * 100).percent() - 3.gpx())
            applyColor()
        }

        bar.onMouseClick { dragging = true }
        bar.onMouseRelease { dragging = false }
        bar.onMouseDrag { mouseX, _, _ ->
            if (!dragging) return@onMouseDrag
            updateFromMouse(mouseX)
        }

        return bar
    }
}
package xyz.vexo.clickgui.components.settings

import java.awt.Color
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.*
import xyz.vexo.clickgui.gpx
import xyz.vexo.clickgui.theme.Theme
import xyz.vexo.config.impl.ColorSetting

/**
 * Color setting component — click the swatch to expand glass RGBA sliders; the swatch updates live.
 */
class ColorSettingComponent(
    private val setting: ColorSetting
) : UIContainer() {

    private var expanded = false
    private val sliderContainer: UIContainer
    private val colorPreview: UIRoundedRectangle

    init {
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
        }.apply { setColor(setting.getCurrentValue()) } childOf this

        colorPreview.onMouseClick { toggleExpanded() }

        sliderContainer = UIContainer().constrain {
            x = 14.gpx()
            y = 32.gpx()
            width = 100.percent() - 28.gpx()
            height = if (setting.allowAlpha) 140.gpx() else 105.gpx()
        } childOf this

        sliderContainer.hide(instantly = true)

        createColorSlider("R", setting.getCurrentValue().red, Color.RED) { value ->
            val c = setting.getCurrentValue()
            val n = Color(value, c.green, c.blue, c.alpha)
            setting.updateValue(n); colorPreview.setColor(n)
        }.constrain { y = 0.gpx() } childOf sliderContainer

        createColorSlider("G", setting.getCurrentValue().green, Color.GREEN) { value ->
            val c = setting.getCurrentValue()
            val n = Color(c.red, value, c.blue, c.alpha)
            setting.updateValue(n); colorPreview.setColor(n)
        }.constrain { y = 35.gpx() } childOf sliderContainer

        createColorSlider("B", setting.getCurrentValue().blue, Color.BLUE) { value ->
            val c = setting.getCurrentValue()
            val n = Color(c.red, c.green, value, c.alpha)
            setting.updateValue(n); colorPreview.setColor(n)
        }.constrain { y = 70.gpx() } childOf sliderContainer

        if (setting.allowAlpha) {
            createColorSlider("A", setting.getCurrentValue().alpha, Color.GRAY) { value ->
                val c = setting.getCurrentValue()
                val n = Color(c.red, c.green, c.blue, value)
                setting.updateValue(n); colorPreview.setColor(n)
            }.constrain { y = 105.gpx() } childOf sliderContainer
        }
    }

    private fun toggleExpanded() {
        expanded = !expanded
        if (expanded) {
            sliderContainer.unhide()
            constrain { height = if (setting.allowAlpha) 180.gpx() else 145.gpx() }
        } else {
            sliderContainer.hide()
            constrain { height = 30.gpx() }
        }
    }

    private fun createColorSlider(label: String, value: Int, tint: Color, onChange: (Int) -> Unit): UIContainer {
        return UIContainer().constrain {
            width = 100.percent()
            height = 30.gpx()
        }.apply {
            UIText(label).constrain {
                x = 0.gpx()
                y = CenterConstraint()
                textScale = 1.gpx()
            }.setColor(Theme.textMuted()) childOf this

            val valueText = UIText(value.toString()).constrain {
                x = 30.gpx()
                y = CenterConstraint()
                textScale = 1.gpx()
            }.apply { setColor(Theme.textPrimary()) } childOf this

            val sliderArea = UIContainer().constrain {
                x = 60.gpx()
                y = CenterConstraint()
                width = 100.percent() - 60.gpx()
                height = 20.gpx()
            } childOf this

            val sliderTrack = UIRoundedRectangle(2f).constrain {
                width = 100.percent()
                height = 4.gpx()
                y = CenterConstraint()
            }.setColor(Theme.sliderTrack()) childOf sliderArea

            val sliderFill = UIRoundedRectangle(2f).constrain {
                width = (value / 255f * 100).percent()
                height = 4.gpx()
                y = CenterConstraint()
            }.setColor(tint) childOf sliderTrack

            val sliderHandle = UIRoundedRectangle(6f).constrain {
                x = (value / 255f * 100).percent() - 6.gpx()
                y = CenterConstraint()
                width = 12.gpx()
                height = 12.gpx()
            }.setColor(Theme.handle()) childOf sliderArea

            var dragging = false
            sliderHandle.onMouseClick { dragging = true }
            sliderTrack.onMouseClick { dragging = true }
            sliderHandle.onMouseRelease { dragging = false }
            sliderTrack.onMouseRelease { dragging = false }

            sliderTrack.onMouseDrag { mouseX, _, _ ->
                if (!dragging) return@onMouseDrag
                val trackWidth = sliderTrack.getWidth()
                val relativeX = mouseX.coerceIn(0f, trackWidth)
                val percentage = relativeX / trackWidth
                val newValue = (percentage * 255).toInt().coerceIn(0, 255)
                onChange(newValue)
                valueText.setText(newValue.toString())
                sliderFill.setWidth((percentage * 100).percent())
                sliderHandle.setX((percentage * 100).percent() - 6.gpx())
            }
        }
    }
}

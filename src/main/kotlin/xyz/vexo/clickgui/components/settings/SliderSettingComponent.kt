package xyz.vexo.clickgui.components.settings

import kotlin.math.roundToInt
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.*
import xyz.vexo.clickgui.gpx
import xyz.vexo.clickgui.theme.Theme
import xyz.vexo.config.impl.SliderSetting

/**
 * Slider setting component — accent fill with a soft glow handle and a live value readout.
 */
class SliderSettingComponent(
    private val setting: SliderSetting
) : UIContainer() {

    init {
        constrain {
            width = 100.percent()
            height = 58.gpx()
        }

        UIText(setting.name).constrain {
            x = 14.gpx()
            y = 8.gpx()
            textScale = 1.gpx()
        }.setColor(Theme.textPrimary()) childOf this

        val valueText = UIText(formatValue(setting.getCurrentValue())).constrain {
            x = 14.gpx(true)
            y = 8.gpx()
            textScale = 1.gpx()
        }.apply { setColor(Theme.accent()) } childOf this

        val sliderContainer = UIContainer().constrain {
            x = 14.gpx()
            y = 28.gpx()
            width = 100.percent() - 28.gpx()
            height = 20.gpx()
        } childOf this

        val sliderTrack = UIRoundedRectangle(3f).constrain {
            width = 100.percent()
            height = 6.gpx()
            y = CenterConstraint()
        }.setColor(Theme.sliderTrack()) childOf sliderContainer

        val startPercentage =
            ((setting.getCurrentValue() - setting.min) / (setting.max - setting.min)).toFloat()

        val sliderFill = UIRoundedRectangle(3f).constrain {
            width = (startPercentage * 100).percent()
            height = 6.gpx()
            y = CenterConstraint()
        }.setColor(Theme.accent()) childOf sliderTrack

        val handleGlow = UIRoundedRectangle(9f).constrain {
            x = (startPercentage * 100).percent() - 9.gpx()
            y = CenterConstraint()
            width = 18.gpx()
            height = 18.gpx()
        }.setColor(Theme.glow(80)) childOf sliderContainer

        val sliderHandle = UIRoundedRectangle(7f).constrain {
            x = (startPercentage * 100).percent() - 7.gpx()
            y = CenterConstraint()
            width = 14.gpx()
            height = 14.gpx()
        }.setColor(Theme.handle()) childOf sliderContainer

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

            val rawValue = setting.min + (setting.max - setting.min) * percentage
            val steps = ((rawValue - setting.min) / setting.increment).roundToInt()
            val newValue = setting.min + (steps * setting.increment)

            setting.updateValue(newValue)
            valueText.setText(formatValue(setting.getCurrentValue()))

            val actualPercentage =
                ((setting.getCurrentValue() - setting.min) / (setting.max - setting.min)).toFloat()

            sliderFill.setWidth((actualPercentage * 100).percent())
            sliderHandle.setX((actualPercentage * 100).percent() - 7.gpx())
            handleGlow.setX((actualPercentage * 100).percent() - 9.gpx())
        }
    }

    private fun formatValue(value: Double): String {
        return when {
            setting.increment >= 1.0 -> String.format("%.0f", value)
            setting.increment >= 0.1 -> String.format("%.1f", value)
            setting.increment >= 0.01 -> String.format("%.2f", value)
            else -> String.format("%.3f", value)
        }
    }
}

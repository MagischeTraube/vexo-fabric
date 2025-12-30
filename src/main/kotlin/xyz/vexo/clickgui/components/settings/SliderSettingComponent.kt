package xyz.vexo.clickgui.components.settings

import kotlin.math.roundToInt
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.*
import xyz.vexo.config.impl.SliderSetting
import xyz.vexo.clickgui.components.ClickGuiColor

/**
 * Slider setting component
 *
 * @param setting The slider setting to be displayed
 */
class SliderSettingComponent(
    private val setting: SliderSetting
) : UIContainer() {

    init {
        constrain {
            width = 100.percent()
            height = 60.pixels()
        }

        UIText(setting.name).constrain {
            x = 10.pixels()
            y = 5.pixels()
        } childOf this

        val valueText = UIText(formatValue(setting.getCurrentValue())).constrain {
            x = 10.pixels(true)
            y = 5.pixels()
        } childOf this

        val sliderContainer = UIContainer().constrain {
            x = 10.pixels()
            y = 25.pixels()
            width = 100.percent() - 20.pixels()
            height = 20.pixels()
        } childOf this

        val sliderTrack = UIBlock(ClickGuiColor.LIGHT_GRAY_BACKGROUND).constrain {
            width = 100.percent()
            height = 4.pixels()
            y = CenterConstraint()
        } childOf sliderContainer

        val startPercentage =
            ((setting.getCurrentValue() - setting.min) / (setting.max - setting.min)).toFloat()

        val sliderFill = UIBlock(ClickGuiColor.ACCENT_COLOR).constrain {
            width = (startPercentage * 100).percent()
            height = 4.pixels()
            y = CenterConstraint()
        } childOf sliderTrack

        val sliderHandle = UIBlock(ClickGuiColor.WHITE_SLIDER_HANDLE_COLOR).constrain {
            x = (startPercentage * 100).percent() - 6.pixels()
            y = CenterConstraint()
            width = 12.pixels()
            height = 12.pixels()
        } childOf sliderContainer

        var dragging = false

        sliderHandle.onMouseClick { dragging = true }
        sliderTrack.onMouseClick { dragging = true }

        sliderHandle.onMouseRelease { dragging = false }
        sliderTrack.onMouseRelease{ dragging = false }

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

            val actualPercentage = ((setting.getCurrentValue() - setting.min) / (setting.max - setting.min)).toFloat()

            sliderFill.setWidth((actualPercentage * 100).percent())
            sliderHandle.setX((actualPercentage * 100).percent() - 6.pixels())
        }
    }

    /**
     * Formats the value based on the increment
     */
    private fun formatValue(value: Double): String {
        return if (setting.increment >= 1.0) {
            String.format("%.0f", value)
        } else if (setting.increment >= 0.1) {
            String.format("%.1f", value)
        } else if (setting.increment >= 0.01) {
            String.format("%.2f", value)
        } else {
            String.format("%.3f", value)
        }
    }
}

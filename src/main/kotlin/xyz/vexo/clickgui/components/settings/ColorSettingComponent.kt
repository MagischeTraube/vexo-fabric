package xyz.vexo.clickgui.components.settings

import java.awt.Color
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.minus
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.pixels
import xyz.vexo.config.impl.ColorSetting
import xyz.vexo.clickgui.components.ClickGuiColor

/**
 * Color setting component
 *
 * @param setting The color setting to be displayed
 */
class ColorSettingComponent(
    private val setting: ColorSetting
) : UIContainer() {

    private var expanded = false

    private var sliderContainer: UIContainer
    private var colorPreview: UIBlock

    init {
        constrain {
            width = 100.percent()
            height = 30.pixels()
        }

        UIText(setting.name).constrain {
            x = 10.pixels()
            y = 5.pixels()
        } childOf this

        colorPreview = UIBlock(setting.getCurrentValue()).constrain {
            x = 10.pixels(true)
            y = 5.pixels()
            width = 30.pixels()
            height = 20.pixels()
        } childOf this

        colorPreview.onMouseClick {
            toggleExpanded()
        }

        sliderContainer = UIContainer().constrain {
            x = 10.pixels()
            y = 30.pixels()
            width = 100.percent() - 20.pixels()
            height = if (setting.allowAlpha) 140.pixels() else 105.pixels()
        } childOf this

        sliderContainer.hide(instantly = true)

        createColorSlider("R", setting.getCurrentValue().red, Color.RED) { value ->
            val newColor = Color(
                value,
                setting.getCurrentValue().green,
                setting.getCurrentValue().blue,
                setting.getCurrentValue().alpha
            )
            setting.updateValue(newColor)
            colorPreview.setColor(newColor)
        }.constrain { y = 0.pixels() } childOf sliderContainer

        createColorSlider("G", setting.getCurrentValue().green, Color.GREEN) { value ->
            val newColor = Color(
                setting.getCurrentValue().red,
                value,
                setting.getCurrentValue().blue,
                setting.getCurrentValue().alpha
            )
            setting.updateValue(newColor)
            colorPreview.setColor(newColor)
        }.constrain { y = 35.pixels() } childOf sliderContainer

        createColorSlider("B", setting.getCurrentValue().blue, Color.BLUE) { value ->
            val newColor = Color(
                setting.getCurrentValue().red,
                setting.getCurrentValue().green,
                value,
                setting.getCurrentValue().alpha
            )
            setting.updateValue(newColor)
            colorPreview.setColor(newColor)
        }.constrain { y = 70.pixels() } childOf sliderContainer

        if (setting.allowAlpha) {
            createColorSlider("A", setting.getCurrentValue().alpha, Color.GRAY) { value ->
                val newColor = Color(
                    setting.getCurrentValue().red,
                    setting.getCurrentValue().green,
                    setting.getCurrentValue().blue,
                    value
                )
                setting.updateValue(newColor)
                colorPreview.setColor(newColor)
            }.constrain { y = 105.pixels() } childOf sliderContainer
        }
    }

    private fun toggleExpanded() {
        expanded = !expanded

        if (expanded) {
            sliderContainer.unhide()
            constrain {
                height = if (setting.allowAlpha) 180.pixels() else 145.pixels()
            }
        } else {
            sliderContainer.hide()
            constrain {
                height = 30.pixels()
            }
        }
    }

    private fun createColorSlider(label: String, value: Int, color: Color, onChange: (Int) -> Unit): UIContainer {
        return UIContainer().constrain {
            width = 100.percent()
            height = 30.pixels()
        }.apply {
            UIText(label).constrain {
                x = 0.pixels()
                y = CenterConstraint()
            } childOf this

            val valueText = UIText(value.toString()).constrain {
                x = 30.pixels()
                y = CenterConstraint()
            } childOf this

            val sliderArea = UIContainer().constrain {
                x = 60.pixels()
                y = CenterConstraint()
                width = 100.percent() - 60.pixels()
                height = 20.pixels()
            } childOf this

            val sliderTrack = UIBlock(ClickGuiColor.LIGHT_GRAY_BACKGROUND).constrain {
                width = 100.percent()
                height = 4.pixels()
                y = CenterConstraint()
            } childOf sliderArea

            val sliderFill = UIBlock(color).constrain {
                width = (value / 255f * 100).percent()
                height = 4.pixels()
                y = CenterConstraint()
            } childOf sliderTrack

            val sliderHandle = UIBlock(ClickGuiColor.WHITE_SLIDER_HANDLE_COLOR).constrain {
                x = (value / 255f * 100).percent() - 6.pixels()
                y = CenterConstraint()
                width = 12.pixels()
                height = 12.pixels()
            } childOf sliderArea

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
                sliderHandle.setX((percentage * 100).percent() - 6.pixels())
            }
        }
    }
}

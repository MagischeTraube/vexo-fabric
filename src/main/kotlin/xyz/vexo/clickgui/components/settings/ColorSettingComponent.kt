package xyz.vexo.clickgui.components.settings

import java.awt.Color
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.UIText
import gg.essential.elementa.dsl.*
import xyz.vexo.clickgui.components.AlphaBar
import xyz.vexo.clickgui.components.HueBar
import xyz.vexo.clickgui.components.SvBox
import xyz.vexo.clickgui.gpx
import xyz.vexo.clickgui.theme.Theme
import xyz.vexo.config.impl.ColorSetting

/**
 * Color setting component - expandable picker with hue bar, saturation/brightness box, and alpha slider.
 */
class ColorSettingComponent(
    private val setting: ColorSetting
) : UIContainer() {

    private var expanded = false
    private val sliderContainer: UIContainer
    private val colorPreview: UIRoundedRectangle

    private var hue: Float = 0f
    private var saturation: Float = 0f
    private var brightness: Float = 1f
    private var alpha: Int = setting.getCurrentValue().alpha

    companion object {
        private const val SPACING = 10f
    }

    private var hueBar: HueBar? = null
    private var svBox: SvBox? = null
    private var alphaBar: AlphaBar? = null

    init {
        val initial = setting.getCurrentValue()

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

        val contentHeight = HueBar.HEIGHT + SPACING + SvBox.HEIGHT +
                if (setting.allowAlpha) SPACING + AlphaBar.HEIGHT else 0f

        sliderContainer = UIContainer().constrain {
            x = 14.gpx()
            y = 32.gpx()
            width = 100.percent() - 28.gpx()
            height = contentHeight.gpx()
        } childOf this

        sliderContainer.hide(instantly = true)

        svBox = SvBox(initial) { newColor ->
            hue = Color.RGBtoHSB(newColor.red, newColor.green, newColor.blue, null)[0]
            applyColor()
        }.constrain {
            x = 0.gpx()
            y = (HueBar.HEIGHT + SPACING).gpx()
            width = 100.percent()
            height = SvBox.HEIGHT.gpx()
        } childOf sliderContainer

        val hsb = Color.RGBtoHSB(initial.red, initial.green, initial.blue, null)
        hue = hsb[0]
        saturation = hsb[1]
        brightness = hsb[2]

        hueBar = HueBar(hue) { newHue ->
            hue = newHue
            svBox!!.updateColor(Color.getHSBColor(hue, saturation, brightness))
            applyColor()
        }.constrain {
            width = 100.percent()
            height = HueBar.HEIGHT.gpx()
        } childOf sliderContainer

        if (setting.allowAlpha) {
            alphaBar = AlphaBar(initial, alpha) { newAlpha ->
                alpha = newAlpha
                applyColor()
            }.constrain {
                x = 0.gpx()
                y = (HueBar.HEIGHT + SPACING + SvBox.HEIGHT + SPACING).gpx()
                width = 100.percent()
                height = AlphaBar.HEIGHT.gpx()
            } childOf sliderContainer
        }
    }

    private fun toggleExpanded() {
        expanded = !expanded
        val contentHeight = HueBar.HEIGHT + SPACING + SvBox.HEIGHT +
                if (setting.allowAlpha) SPACING + AlphaBar.HEIGHT else 0f
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
        alphaBar?.updateColor(rgb)
        alphaBar?.setAlpha(alpha)
    }
}

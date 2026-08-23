package xyz.vexo.clickgui.components.panels

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import xyz.vexo.clickgui.GuiPrefs
import xyz.vexo.clickgui.components.AlphaBar
import xyz.vexo.clickgui.components.HueBar
import xyz.vexo.clickgui.components.SvBox
import xyz.vexo.clickgui.gpx
import xyz.vexo.clickgui.gsib
import xyz.vexo.clickgui.theme.Theme
import xyz.vexo.clickgui.theme.colorTo
import xyz.vexo.clickgui.theme.xTo
import java.awt.Color
import kotlin.math.roundToInt

class GuiSettingsOverlay(
    private val onApply: () -> Unit
) : UIContainer() {

    init {
        constrain {
            width = 100.percent()
            height = 100.percent()
        }

        val scrim = UIBlock(Theme.overlayScrim()).constrain {
            width = 100.percent()
            height = 100.percent()
        } childOf this
        scrim.onMouseClick { dismiss() }

        val card = UIRoundedRectangle(12f).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 340.gpx()
            height = 420.gpx()
        }.setColor(Theme.panelDark()) childOf this
        card.enableEffect(OutlineEffect(Theme.glow(140), 1f))
        card.onMouseClick { it.stopPropagation() }

        UIText("GUI Settings").constrain {
            x = 18.gpx()
            y = 16.gpx()
            textScale = 1.6.gpx()
        }.setColor(Theme.accent()) childOf card

        UIText("Tune the look & feel").constrain {
            x = 18.gpx()
            y = 38.gpx()
            textScale = 0.9.gpx()
        }.setColor(Theme.textMuted()) childOf card

        val content = UIContainer().constrain {
            x = 14.gpx()
            y = 58.gpx()
            width = 100.percent() - 28.gpx()
            height = 100.percent() - 104.gpx()
        } childOf card

        var first = true
        fun nextY() = if (first) 0.pixels().also { first = false } else gsib(6f)

        toggleRow("Animations", GuiPrefs.animations) { GuiPrefs.animations = it }
            .constrain { y = nextY() } childOf content
        toggleRow("Glass / Transparency", GuiPrefs.blurGlass) { GuiPrefs.blurGlass = it }
            .constrain { y = nextY() } childOf content
        toggleRow("Shadows", GuiPrefs.shadows) { GuiPrefs.shadows = it }
            .constrain { y = nextY() } childOf content
        toggleRow("Glow", GuiPrefs.glow) { GuiPrefs.glow = it }
            .constrain { y = nextY() } childOf content
        toggleRow("Tooltips", GuiPrefs.tooltips) { GuiPrefs.tooltips = it }
            .constrain { y = nextY() } childOf content
        scaleRow().constrain { y = nextY() } childOf content
        accentRow().constrain { y = nextY() } childOf content

        val done = UIRoundedRectangle(8f).constrain {
            x = CenterConstraint()
            y = 14.gpx(true)
            width = 120.gpx()
            height = 26.gpx()
        }.setColor(Theme.accent()) childOf card
        UIText("Done").constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            textScale = 1.gpx()
        }.setColor(Theme.textOnAccent()) childOf done
        done.onMouseEnter { done.colorTo(Theme.accentHover()) }
        done.onMouseLeave { done.colorTo(Theme.accent()) }
        done.onMouseClick {
            it.stopPropagation()
            dismiss()
        }
    }

    private fun dismiss() {
        GuiPrefs.save()
        parent.removeChild(this)
        onApply()
    }

    private fun toggleRow(label: String, initial: Boolean, onSet: (Boolean) -> Unit): UIComponent {
        return UIContainer().constrain {
            width = 100.percent()
            height = 24.gpx()
        }.apply {
            UIText(label).constrain {
                x = 2.gpx()
                y = CenterConstraint()
                textScale = 1.gpx()
            }.setColor(Theme.textPrimary()) childOf this

            var value = initial
            val toggle = UIContainer().constrain {
                x = 0.pixels(true)
                y = CenterConstraint()
                width = 38.gpx()
                height = 18.gpx()
            } childOf this

            val bg = UIRoundedRectangle(9f).constrain {
                width = 100.percent()
                height = 100.percent()
            }.setColor(if (value) Theme.accent() else Theme.toggleOff()) childOf toggle

            val knob = UIRoundedRectangle(7f).constrain {
                x = if (value) 21.gpx() else 2.gpx()
                y = 2.gpx()
                width = 14.gpx()
                height = 14.gpx()
            }.setColor(Theme.handle()) childOf toggle

            toggle.onMouseClick {
                value = !value
                onSet(value)
                bg.colorTo(if (value) Theme.accent() else Theme.toggleOff())
                knob.xTo(if (value) 21.gpx() else 2.gpx())
            }
        }
    }

    private fun scaleRow(): UIComponent {
        return UIContainer().constrain {
            width = 100.percent()
            height = 24.gpx()
        }.apply {
            UIText("Scale").constrain {
                x = 2.gpx()
                y = CenterConstraint()
                textScale = 1.gpx()
            }.setColor(Theme.textPrimary()) childOf this

            val row = UIContainer().constrain {
                x = 0.pixels(true)
                y = CenterConstraint()
                width = 80.gpx()
                height = 18.gpx()
            } childOf this

            val valueText = UIText(GuiPrefs.scale.toString()).constrain {
                x = CenterConstraint()
                y = CenterConstraint()
                textScale = 1.gpx()
            }.apply { setColor(Theme.accent()) } childOf row

            fun stepBtn(symbol: String, xRight: Boolean, delta: Int) {
                val b = UIRoundedRectangle(5f).constrain {
                    x = if (xRight) 0.pixels(true) else 0.pixels()
                    y = CenterConstraint()
                    width = 18.gpx()
                    height = 18.gpx()
                }.setColor(Theme.card()) childOf row

                UIText(symbol).constrain {
                    x = CenterConstraint()
                    y = CenterConstraint()
                    textScale = 1.gpx()
                }.setColor(Theme.textPrimary()) childOf b

                b.onMouseEnter { b.colorTo(Theme.accent()) }
                b.onMouseLeave { b.colorTo(Theme.card()) }
                b.onMouseClick {
                    GuiPrefs.scale += delta
                    valueText.setText(GuiPrefs.scale.toString())
                }
            }

            stepBtn("-", false, -1)
            stepBtn("+", true, 1)
        }
    }

    private fun accentRow(): UIComponent {
        return UIContainer().constrain {
            width = 100.percent()
            height = 120.gpx()
        }.apply {
            UIText("Accent color").constrain {
                x = 2.gpx()
                y = 0.pixels()
                textScale = 1.gpx()
            }.setColor(Theme.textPrimary()) childOf this

            val preview = UIRoundedRectangle(4f).constrain {
                x = 0.pixels(true)
                y = 0.pixels()
                width = 22.gpx()
                height = 14.gpx()
            }.setColor(GuiPrefs.accentColor) childOf this

            val hsb = Color.RGBtoHSB(
                GuiPrefs.accentColor.red,
                GuiPrefs.accentColor.green,
                GuiPrefs.accentColor.blue,
                null
            )
            var hue = hsb[0]
            var svBox: SvBox? = null

            val hueBar = HueBar(hue) { newHue ->
                hue = newHue
                svBox!!.updateColor(Color.getHSBColor(hue, 1f, 1f))
                GuiPrefs.accentColor = Color.getHSBColor(hue, 1f, 1f)
                preview.setColor(GuiPrefs.accentColor)
            }.constrain {
                x = 0.gpx()
                y = 18.gpx()
                width = 100.percent()
                height = HueBar.HEIGHT.gpx()
            } childOf this

            svBox = SvBox(GuiPrefs.accentColor, onChange = { newColor ->
                GuiPrefs.accentColor = newColor
                preview.setColor(newColor)
            }).constrain {
                x = 0.gpx()
                y = (18f + HueBar.HEIGHT + 4).gpx()
                width = 100.percent()
                height = SvBox.HEIGHT.gpx()
            } childOf this
        }
    }
}

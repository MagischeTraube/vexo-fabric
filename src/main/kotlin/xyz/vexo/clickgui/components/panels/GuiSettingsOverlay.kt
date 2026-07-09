package xyz.vexo.clickgui.components.panels

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import xyz.vexo.clickgui.GuiPrefs
import xyz.vexo.clickgui.gpx
import xyz.vexo.clickgui.gsib
import xyz.vexo.clickgui.theme.Theme
import xyz.vexo.clickgui.theme.colorTo
import xyz.vexo.clickgui.theme.xTo
import java.awt.Color
import kotlin.math.roundToInt

/**
 * Modal glass overlay for tuning the GUI's own presentation: the effect toggles (animations, glass,
 * shadows, glow, tooltips), the accent color and the GUI scale. Writes straight to [GuiPrefs].
 *
 * Structural changes (glass transparency, accent, scale) are applied by reopening the GUI via
 * [onApply] when the overlay is dismissed.
 */
class GuiSettingsOverlay(
    private val onApply: () -> Unit
) : UIContainer() {

    init {
        constrain {
            width = 100.percent()
            height = 100.percent()
        }

        // Scrim — clicking it dismisses.
        val scrim = UIBlock(Theme.overlayScrim()).constrain {
            width = 100.percent()
            height = 100.percent()
        } childOf this
        scrim.onMouseClick { dismiss() }

        val card = UIRoundedRectangle(12f).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 340.gpx()
            height = 350.gpx()
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

        // Done button.
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

            val valueText = UIText(GuiPrefs.scale.toString()).constrain {
                x = 0.pixels(true)
                y = CenterConstraint()
                textScale = 1.gpx()
            }.apply { setColor(Theme.accent()) } childOf this

            // 1..5 stepper buttons.
            val row = UIContainer().constrain {
                x = 18.gpx(true)
                y = CenterConstraint()
                width = 70.gpx()
                height = 18.gpx()
            } childOf this

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
                    GuiPrefs.scale = GuiPrefs.scale + delta
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
            height = 56.gpx()
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

            fun channel(label: String, yy: Float, get: () -> Int, set: (Int) -> Unit) {
                val c = UIContainer().constrain {
                    x = 0.pixels()
                    y = yy.gpx()
                    width = 100.percent()
                    height = 16.gpx()
                } childOf this
                UIText(label).constrain { x = 0.pixels(); y = CenterConstraint(); textScale = 1.gpx() }
                    .setColor(Theme.textMuted()) childOf c
                val track = UIRoundedRectangle(2f).constrain {
                    x = 18.gpx()
                    y = CenterConstraint()
                    width = 100.percent() - 18.gpx()
                    height = 4.gpx()
                }.setColor(Theme.sliderTrack()) childOf c
                val handle = UIRoundedRectangle(6f).constrain {
                    x = (get() / 255f * 100).percent()
                    y = CenterConstraint()
                    width = 10.gpx()
                    height = 10.gpx()
                }.setColor(Theme.handle()) childOf track
                var dragging = false
                track.onMouseClick { dragging = true }
                track.onMouseRelease { dragging = false }
                handle.onMouseClick { dragging = true }
                handle.onMouseRelease { dragging = false }
                track.onMouseDrag { mouseX, _, _ ->
                    if (!dragging) return@onMouseDrag
                    val w = track.getWidth()
                    val pct = mouseX.coerceIn(0f, w) / w
                    set((pct * 255).roundToInt().coerceIn(0, 255))
                    handle.setX((pct * 100).percent())
                    preview.setColor(GuiPrefs.accentColor)
                }
            }

            channel("R", 16f, { GuiPrefs.accentColor.red }) {
                GuiPrefs.accentColor = Color(it, GuiPrefs.accentColor.green, GuiPrefs.accentColor.blue)
            }
            channel("G", 32f, { GuiPrefs.accentColor.green }) {
                GuiPrefs.accentColor = Color(GuiPrefs.accentColor.red, it, GuiPrefs.accentColor.blue)
            }
            channel("B", 48f, { GuiPrefs.accentColor.blue }) {
                GuiPrefs.accentColor = Color(GuiPrefs.accentColor.red, GuiPrefs.accentColor.green, it)
            }
        }
    }
}

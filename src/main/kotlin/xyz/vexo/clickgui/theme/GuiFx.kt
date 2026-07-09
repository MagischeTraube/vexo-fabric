package xyz.vexo.clickgui.theme

import gg.essential.elementa.UIComponent
import gg.essential.elementa.constraints.HeightConstraint
import gg.essential.elementa.constraints.WidthConstraint
import gg.essential.elementa.constraints.XConstraint
import gg.essential.elementa.constraints.YConstraint
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.animate
import gg.essential.elementa.dsl.toConstraint
import xyz.vexo.clickgui.GuiPrefs
import java.awt.Color

/**
 * Animation-aware helpers. Each respects [GuiPrefs.animations]: when animations are disabled they
 * fall back to an instant set, so the whole "smooth" layer can be turned off from the GUI settings.
 */

fun UIComponent.colorTo(target: Color, time: Float = 0.18f): UIComponent {
    if (GuiPrefs.animations) animate { setColorAnimation(Animations.OUT_EXP, time, target.toConstraint()) }
    else setColor(target)
    return this
}

fun UIComponent.xTo(target: XConstraint, time: Float = 0.18f): UIComponent {
    if (GuiPrefs.animations) animate { setXAnimation(Animations.OUT_EXP, time, target) }
    else setX(target)
    return this
}

fun UIComponent.yTo(target: YConstraint, time: Float = 0.18f): UIComponent {
    if (GuiPrefs.animations) animate { setYAnimation(Animations.OUT_EXP, time, target) }
    else setY(target)
    return this
}

fun UIComponent.widthTo(target: WidthConstraint, time: Float = 0.18f): UIComponent {
    if (GuiPrefs.animations) animate { setWidthAnimation(Animations.OUT_EXP, time, target) }
    else setWidth(target)
    return this
}

fun UIComponent.heightTo(target: HeightConstraint, time: Float = 0.2f): UIComponent {
    if (GuiPrefs.animations) animate { setHeightAnimation(Animations.OUT_EXP, time, target) }
    else setHeight(target)
    return this
}

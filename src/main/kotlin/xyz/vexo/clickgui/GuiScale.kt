package xyz.vexo.clickgui

import gg.essential.elementa.constraints.PixelConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.pixels


/**
 * Returns the current GUI scale multiplier from [GuiPrefs.scale].
 */
val guiScale: Float get() = GuiPrefs.sizeMultiplier()

/**
 * Scales a pixel value by the current GUI scale and returns a [PixelConstraint].
 *
 * @param alignOpposite Whether to align the constraint to the opposite edge
 * @param alignOutside Whether to align the constraint outside the parent
 */
fun Number.gpx(alignOpposite: Boolean = false, alignOutside: Boolean = false): PixelConstraint =
    (toFloat() * guiScale).pixels(alignOpposite, alignOutside)

/**
 * Creates a scaled [SiblingConstraint] based on the current GUI scale.
 *
 * @param padding The base padding value to scale
 * @param alignOpposite Whether to align the constraint to the opposite edge
 */
fun gsib(padding: Float = 0f, alignOpposite: Boolean = false): SiblingConstraint =
    SiblingConstraint(padding * guiScale, alignOpposite)
